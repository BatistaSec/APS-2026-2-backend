package com.app.service.openweather;

import com.app.model.MedicaoAmbiental;
import com.app.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenWeatherClientImpl implements OpenWeatherClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String[] ROSA_DOS_VENTOS = {"N", "NE", "L", "SE", "S", "SO", "O", "NO"};

    private final String apiKey;
    private final HttpClient httpClient;

    private final ConcurrentHashMap<String, MedicaoAmbiental> cacheUltimasMedicoes = new ConcurrentHashMap<>();

    public OpenWeatherClientImpl() {
        this(obterApiKeyConfigurada(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public OpenWeatherClientImpl(String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public OpenWeatherClientImpl(String apiKey, HttpClient httpClient) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : null;
        this.httpClient = httpClient;
    }

    private static String obterApiKeyConfigurada() {
        String env = System.getenv("OPENWEATHER_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("openweather.api.key");
        if (prop != null && !prop.isBlank()) return prop.trim();
        // Fallback para projeto acadêmico: chave padrão (sem variável de ambiente necessária)
        return "fd8737c649a4176f6c40c6411e959a59";
    }

    @Override
    public MedicaoAmbiental buscarClima(String cidade) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(com.app.util.i18n.I18n.get("openweather.api_key_ausente"));
        }
        String cidadeAlvo = (cidade != null && !cidade.isBlank()) ? cidade.trim() : "Sao Paulo,BR";
        String encodedCidade = URLEncoder.encode(cidadeAlvo, StandardCharsets.UTF_8);
        String url = String.format("%s?q=%s&appid=%s&units=metric&lang=pt_br", BASE_URL, encodedCidade, apiKey);

        int maxTentativas = 2;
        Exception ultimaFalha = null;

        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                String body = response.body();

                if (status == 200) {
                    MedicaoAmbiental medicao = converterJsonParaMedicao(body);
                    cacheUltimasMedicoes.put(cidadeAlvo.toLowerCase(Locale.ROOT), medicao);
                    return medicao;
                }

                String msgErro = JsonUtil.extrairTexto(body, "message");
                if (status == 404) {
                    throw new IllegalArgumentException(com.app.util.i18n.I18n.get("openweather.cidade_nao_encontrada", cidadeAlvo));
                } else if (status == 401) {
                    throw new IllegalStateException(com.app.util.i18n.I18n.get("openweather.nao_autorizado"));
                } else {
                    throw new RuntimeException(com.app.util.i18n.I18n.get("openweather.erro_servico", status, msgErro));
                }

            } catch (IllegalArgumentException | IllegalStateException e) {
                throw e;
            } catch (IOException e) {
                ultimaFalha = e;
                if (tentativa < maxTentativas) {
                    try {
                        Thread.sleep(300); // Backoff simples
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(com.app.util.i18n.I18n.get("openweather.interrompido"), e);
            } catch (Exception e) {
                ultimaFalha = e;
                break;
            }
        }

        MedicaoAmbiental emCache = cacheUltimasMedicoes.get(cidadeAlvo.toLowerCase(Locale.ROOT));
        if (emCache != null) {
            System.err.println("[RESILIÊNCIA] Utilizando medição em cache para '" + cidadeAlvo + "' devido a indisponibilidade externa: " +
                    (ultimaFalha != null ? ultimaFalha.getMessage() : "Tempo limite esgotado"));
            return emCache;
        }

        String erroFinal = (ultimaFalha != null) ? ultimaFalha.getMessage() : "Erro desconhecido";
        throw new RuntimeException(com.app.util.i18n.I18n.get("openweather.falha_comunicacao", erroFinal), ultimaFalha);
    }

    public MedicaoAmbiental converterJsonParaMedicao(String json) {
        String nomeCidade = JsonUtil.extrairTexto(json, "name");
        if (nomeCidade == null || nomeCidade.isBlank()) {
            nomeCidade = "DESCONHECIDO";
        }

        Double temp = JsonUtil.extrairNumero(json, "temp");
        Double feelsLike = JsonUtil.extrairNumero(json, "feels_like");
        Double umidade = JsonUtil.extrairNumero(json, "humidity");
        Double pressao = JsonUtil.extrairNumero(json, "pressure");
        Double ventoVelMps = JsonUtil.extrairNumero(json, "speed");
        Double ventoDeg = JsonUtil.extrairNumero(json, "deg");

        double ventoKmH = (ventoVelMps != null) ? Math.round(ventoVelMps * 3.6 * 100.0) / 100.0 : 0.0;
        String direcaoVento = (ventoDeg != null) ? converterGrausParaRosa(ventoDeg) : "N";

        Integer weatherId = extrairWeatherId(json);
        boolean chuvaDetectada = (weatherId != null && weatherId >= 200 && weatherId < 700) || json.contains("\"rain\":");

        Double indiceUv = JsonUtil.extrairNumero(json, "uvi");
        if (indiceUv == null) {
            indiceUv = estimarIndiceUv(json, temp);
        }

        String estacaoId = "OPENWEATHER-" + normalizarParaId(nomeCidade);

        return new MedicaoAmbiental(
                estacaoId,
                nomeCidade,
                LocalDateTime.now(),
                temp != null ? round(temp) : 0.0,
                umidade != null ? round(umidade) : 0.0,
                chuvaDetectada,
                ventoKmH,
                pressao != null ? round(pressao) : 1013.25,
                direcaoVento,
                round(indiceUv),
                feelsLike != null ? round(feelsLike) : temp
        );
    }

    public static String converterGrausParaRosa(double graus) {
        int indice = (int) Math.round(((graus % 360) / 45.0)) % 8;
        if (indice < 0) indice += 8;
        return ROSA_DOS_VENTOS[indice];
    }

    private Double estimarIndiceUv(String json, Double temp) {
        Double nuvens = JsonUtil.extrairNumero(json, "all");
        double baseTemp = (temp != null) ? temp : 20.0;
        double cobertura = (nuvens != null) ? nuvens : 50.0;
        double uv = Math.max(0.0, ((baseTemp - 15.0) * 0.35 + 2.5) * (1.0 - (cobertura * 0.007)));
        return Math.min(12.0, Math.max(0.0, uv));
    }

    private String normalizarParaId(String nome) {
        if (nome == null) return "DESCONHECIDO";
        String desacentuado = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return desacentuado.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private Integer extrairWeatherId(String json) {
        Pattern pattern = Pattern.compile("\"weather\"\\s*:\\s*\\[\\s*\\{[^}]*\"id\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private double round(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
