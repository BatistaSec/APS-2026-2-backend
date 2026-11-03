package com.app;

import com.app.handler.MedicaoApiHandler;
import com.app.handler.health.HealthHandler;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.app.handler.proxy.FrontendStaticHandler;
import com.app.handler.proxy.WeatherProxyHandler;
import com.app.service.MonitoramentoService;
import com.app.service.coleta.ColetorAgendadoService;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.logging.AsyncLogger;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORTA_PADRAO = 8080;

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pt", "BR"));

        AsyncLogger.info("==================================================================");
        AsyncLogger.info("  SISTEMA DE MONITORAMENTO AMBIENTAL — APS                       ");
        AsyncLogger.info("  Java 21 + Hibernate 6 + MySQL + OpenWeatherMap                 ");
        AsyncLogger.info("==================================================================");

        try {
            AsyncLogger.info("[1/4] Conectando ao banco de dados...");
            DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();
            connectionManager.getSessionFactory();

            AsyncLogger.info("[2/4] Inicializando serviços...");
            MonitoramentoService monitoramentoService = new MonitoramentoService();

            AsyncLogger.info("[3/4] Iniciando coletor agendado (10s)...");
            ColetorAgendadoService coletorService = monitoramentoService.getColetorService();
            coletorService.iniciar();

            int porta = PORTA_PADRAO;
            String portaEnv = System.getenv("PORT");
            if (portaEnv != null && !portaEnv.isBlank()) {
                try { porta = Integer.parseInt(portaEnv); } catch (NumberFormatException ignored) {}
            }

            Path frontendDir = Paths.get(System.getProperty("frontend.dir",
                    Paths.get("").toAbsolutePath().resolve("frontend").toString()));

            HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            MedicaoApiHandler apiHandler     = new MedicaoApiHandler(monitoramentoService);
            HealthHandler     healthHandler  = new HealthHandler();
            WeatherProxyHandler proxyHandler = new WeatherProxyHandler();
            FrontendStaticHandler frontendHandler = new FrontendStaticHandler(frontendDir);

            server.createContext("/", exchange -> {
                HttpRequest req   = new HttpRequestImpl(exchange);
                HttpResponseSender send = new HttpResponseSenderImpl(exchange);
                String path = req.getPath();

                if ("/".equals(path) || "/index.html".equals(path)) {
                    frontendHandler.handle(exchange);
                    return;
                }

                if ("/api".equals(path)) {
                    send.sucesso("""
                        {
                          "servico": "API Estação de Monitoramento Ambiental (APS)",
                          "versao": "1.0.0",
                          "status": "online",
                          "endpoints": [
                            "GET  /health",
                            "GET  /api/v1/medicoes",
                            "GET  /api/v1/medicoes/{id}",
                            "GET  /api/v1/medicoes/atual?cidade={cidade}",
                            "GET  /api/v1/medicoes/estatisticas",
                            "GET  /api/v1/cidades",
                            "GET  /api/v1/cidades/{cidade}/medicoes",
                            "GET  /api/v1/cidades/{cidade}/estatisticas",
                            "GET  /api/v1/estacoes/{estacaoId}/medicoes",
                            "POST /api/v1/medicoes",
                            "POST /api/v1/sincronizacoes",
                            "POST /api/v1/coletor/cidades",
                            "POST /api/v1/coletor/execucoes",
                            "DELETE /api/v1/medicoes/{id}",
                            "DELETE /api/v1/medicoes?cidade={cidade}"
                          ]
                        }
                        """);
                    return;
                }

                send.naoEncontrado("Rota não encontrada");
            });

            server.createContext("/health",        healthHandler);
            server.createContext("/api/v1/health", healthHandler);
            server.createContext("/api/v1/medicoes",       apiHandler);
            server.createContext("/api/v1/cidades",        apiHandler);
            server.createContext("/api/v1/estacoes",       apiHandler);
            server.createContext("/api/v1/sincronizacoes", apiHandler);
            server.createContext("/api/v1/coletor",        apiHandler);
            server.createContext("/api/medicoes",  apiHandler);
            server.createContext("/api/coletor",   apiHandler);
            server.createContext("/proxy",    proxyHandler);
            server.createContext("/frontend", frontendHandler);

            server.start();

            AsyncLogger.info("[4/4] Servidor iniciado com sucesso!");
            AsyncLogger.info("  URL:      http://localhost:" + porta + "/");
            AsyncLogger.info("  Frontend: http://localhost:" + porta + "/frontend/");
            AsyncLogger.info("  Health:   http://localhost:" + porta + "/health");
            AsyncLogger.info("  Cidades:  http://localhost:" + porta + "/api/v1/cidades");
            AsyncLogger.info("  Coletor:  " + coletorService.getCidades());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                AsyncLogger.info("Encerrando...");
                coletorService.parar();
                server.stop(1);
                connectionManager.shutdown();
                AsyncLogger.shutdown();
            }));

        } catch (IOException e) {
            AsyncLogger.error("Erro ao iniciar o servidor: " + e.getMessage(), e);
        } catch (Throwable t) {
            AsyncLogger.error("Falha crítica na inicialização: " + t.getMessage(), t);
            t.printStackTrace();
        }
    }
}
