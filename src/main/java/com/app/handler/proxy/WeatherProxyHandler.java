package com.app.handler.proxy;

import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

public class WeatherProxyHandler implements HttpHandler {

    private static final String GEO_URL      = "https://api.openweathermap.org/geo/1.0/direct";
    private static final String WEATHER_URL  = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    private final String apiKey;
    private final HttpClient httpClient;

    public WeatherProxyHandler() {
        this(obterApiKey(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public WeatherProxyHandler(String apiKey, HttpClient httpClient) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey.trim() : null;
        this.httpClient = httpClient;
    }

    private static String obterApiKey() {
        String env = System.getenv("OPENWEATHER_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty("openweather.api.key");
        if (prop != null && !prop.isBlank()) return prop.trim();
        return "fd8737c649a4176f6c40c6411e959a59";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            send.noContent();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send.metodoNaoPermitido();
            return;
        }

        if (apiKey == null) {
            // tiles não precisam de API key — deixa passar
            String path0 = exchange.getRequestURI().getPath();
            if (path0.startsWith("/proxy/tiles/")) {
                try { proxyTile(path0, exchange); } catch (Exception e) {
                    new HttpResponseSenderImpl(exchange).erroInterno("Erro ao buscar tile: " + e.getMessage());
                }
                return;
            }
            send.erroInterno("Chave da OpenWeatherMap nao configurada no servidor.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());

        try {
            if (path.endsWith("/geo")) {
                proxyGeo(params, exchange, send);
            } else if (path.endsWith("/weather")) {
                proxyOWM(WEATHER_URL, params, exchange, send);
            } else if (path.endsWith("/forecast")) {
                proxyOWM(FORECAST_URL, params, exchange, send);
            } else if (path.startsWith("/proxy/tiles/")) {
                proxyTile(path, exchange);
            } else {
                send.naoEncontrado("Endpoint de proxy nao reconhecido: " + path);
            }
        } catch (Exception e) {
            send.erroInterno("Erro ao consultar OpenWeatherMap: " + e.getMessage());
        }
    }

    private void proxyGeo(Map<String, String> params, HttpExchange exchange, HttpResponseSender send) throws Exception {
        String q = params.getOrDefault("q", "");
        String limit = params.getOrDefault("limit", "5");
        String url = String.format("%s?q=%s&limit=%s&appid=%s",
                GEO_URL,
                URLEncoder.encode(q, StandardCharsets.UTF_8),
                URLEncoder.encode(limit, StandardCharsets.UTF_8),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        encaminharResposta(url, exchange, send);
    }

    private void proxyOWM(String baseUrl, Map<String, String> params, HttpExchange exchange, HttpResponseSender send) throws Exception {
        String lat = params.getOrDefault("lat", "");
        String lon = params.getOrDefault("lon", "");
        String units = params.getOrDefault("units", "metric");
        String lang = params.getOrDefault("lang", "pt_br");

        String url = String.format("%s?lat=%s&lon=%s&units=%s&lang=%s&appid=%s",
                baseUrl, lat, lon,
                URLEncoder.encode(units, StandardCharsets.UTF_8),
                URLEncoder.encode(lang, StandardCharsets.UTF_8),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        encaminharResposta(url, exchange, send);
    }

    private void encaminharResposta(String url, HttpExchange exchange, HttpResponseSender send) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("Accept-Encoding", "identity")
                .GET()
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int status = resp.statusCode();
        byte[] body = resp.body();

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept-Encoding");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");

        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    /**
     * Proxy de tiles OpenStreetMap — repassa a requisição com User-Agent correto,
     * evitando o bloqueio que ocorre quando o browser acessa diretamente de localhost.
     * Ex: /proxy/tiles/4/7/6.png → https://tile.openstreetmap.org/4/7/6.png
     */
    private void proxyTile(String path, HttpExchange exchange) throws Exception {
        // Extrai /{z}/{x}/{y}.png de /proxy/tiles/{z}/{x}/{y}.png
        String tilePath = path.replaceFirst("^/proxy/tiles", "");
        // Valida: só dígitos, barras e .png
        if (!tilePath.matches("/\\d+/\\d+/\\d+\\.png")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String url = "https://tile.openstreetmap.org" + tilePath;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "APS-Estacao-Ambiental/1.0 (academic project)")
                .header("Accept", "image/png")
                .GET()
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        byte[] body = resp.body();

        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        // Cache de 24h para tiles — reduz carga no OSM
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        exchange.sendResponseHeaders(resp.statusCode(), body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isBlank()) return Map.of();
        return Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .filter(p -> p.length == 2)
                .collect(Collectors.toMap(
                        p -> java.net.URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                        p -> java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8),
                        (a, b) -> a
                ));
    }
}