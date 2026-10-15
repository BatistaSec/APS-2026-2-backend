package com.app.service.coleta;

import com.app.model.MedicaoAmbiental;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.openweather.OpenWeatherClientImpl;
import com.app.service.registro.RegistroMedicaoService;
import com.app.service.registro.RegistroMedicaoServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class ColetorAgendadoServiceImpl implements ColetorAgendadoService {

    private static final List<String> CIDADES_INICIAIS = List.of(
            "Curitiba,BR",
            "Sao Paulo,BR",
            "Rio de Janeiro,BR",
            "Brasilia,BR",
            "Tokyo,JP",
            "London,GB",
            "Miami,US"
    );

    private final OpenWeatherClient openWeatherClient;
    private final RegistroMedicaoService registroMedicaoService;
    private final List<String> cidades;
    private final AtomicInteger indiceCidade = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;

    public ColetorAgendadoServiceImpl() {
        this(new OpenWeatherClientImpl(), new RegistroMedicaoServiceImpl(), carregarCidades());
    }

    public ColetorAgendadoServiceImpl(OpenWeatherClient openWeatherClient, RegistroMedicaoService registroMedicaoService) {
        this(openWeatherClient, registroMedicaoService, carregarCidades());
    }

    public ColetorAgendadoServiceImpl(OpenWeatherClient openWeatherClient, RegistroMedicaoService registroMedicaoService, List<String> cidadesIniciais) {
        this.openWeatherClient = openWeatherClient;
        this.registroMedicaoService = registroMedicaoService;
        this.cidades = new CopyOnWriteArrayList<>(
                (cidadesIniciais != null && !cidadesIniciais.isEmpty()) ? cidadesIniciais : CIDADES_INICIAIS
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Coletor-OpenWeather-10s");
            t.setDaemon(true);
            return t;
        });
    }

    private static List<String> carregarCidades() {
        String envCidades = System.getenv("OPENWEATHER_CIDADES");
        if (envCidades != null && !envCidades.isBlank()) {
            return Arrays.stream(envCidades.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        return CIDADES_INICIAIS;
    }

    @Override
    public void iniciar() {
        System.out.println("[+] Iniciando Coletor Agendado de 10s (OpenWeatherMap)...");
        System.out.println("    Cidades no pool inicial: " + cidades);
        scheduler.scheduleAtFixedRate(this::executarCicloColeta, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void executarCicloColeta() {
        if (cidades.isEmpty()) return;

        int idx = Math.floorMod(indiceCidade.getAndIncrement(), cidades.size());
        String cidadeAlvo = cidades.get(idx);

        try {
            MedicaoAmbiental leitura = openWeatherClient.buscarClima(cidadeAlvo);
            MedicaoAmbiental salva = registroMedicaoService.registrar(leitura);

            String local = (salva.getCidade() != null && !salva.getCidade().isBlank()) ? salva.getCidade() : salva.getEstacaoId();
            System.out.printf("[COLETA 10s] [%s] ID: #%d | 🌡️ %.1f°C (Sensação: %.1f°C) | 💧 %.1f%% | 🌧️ %s | 💨 %.1f km/h (%s)%n",
                    local,
                    salva.getId(),
                    salva.getTemperatura(),
                    salva.getSensacaoTermica(),
                    salva.getUmidade(),
                    Boolean.TRUE.equals(salva.getPrecipitacao()) ? "Chuva" : "Sem Chuva",
                    salva.getVelocidadeVento(),
                    salva.getDirecaoVento());

        } catch (Exception e) {
            System.err.println("[-] Falha na coleta automática para '" + cidadeAlvo + "': " + e.getMessage());
        }
    }

    @Override
    public void adicionarCidade(String cidade) {
        if (cidade != null && !cidade.isBlank() && !cidades.contains(cidade.trim())) {
            cidades.add(cidade.trim());
            System.out.println("[+] Nova cidade adicionada ao coletor: " + cidade.trim());
        }
    }

    @Override
    public boolean removerCidade(String cidade) {
        return cidades.remove(cidade);
    }

    @Override
    public List<String> getCidades() {
        return Collections.unmodifiableList(new ArrayList<>(cidades));
    }

    @Override
    public void parar() {
        System.out.println("[*] Encerrando Coletor Agendado...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
