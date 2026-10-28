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
import com.app.util.EnvLoader;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.i18n.I18n;
import com.app.util.logging.AsyncLogger;
import com.sun.net.httpserver.HttpServer;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORTA_PADRAO = 8080;

    public static void main(String[] args) {
        // Carrega credenciais do .env (gitignored) antes de qualquer uso do Hibernate/OWM
        EnvLoader.carregar();

        Locale.setDefault(I18n.LOCALE_PADRAO);

        AsyncLogger.info("==================================================================");
        AsyncLogger.info("     SISTEMA DE ESTAÇÃO DE MONITORAMENTO AMBIENTAL (APS)          ");
        AsyncLogger.info("     Backend Java 21 + Hibernate 6 + MySQL                        ");
        AsyncLogger.info("     Cache em Memória, GZIP, Logging Assíncrono                   ");
        AsyncLogger.info("==================================================================");

        try {
            AsyncLogger.info("[1/4] Inicializando Pool de Conexões HikariCP e Hibernate...");
            DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance();
            connectionManager.getSessionFactory();

            AsyncLogger.info("[2/4] Inicializando camadas DAO (SRP) e Serviços Especializados...");
            MonitoramentoService monitoramentoService = new MonitoramentoService();

            AsyncLogger.info("[3/4] Inicializando Coletor Agendado (ciclos a cada 10s)...");
            ColetorAgendadoService coletorService = monitoramentoService.getColetorService();
            coletorService.iniciar();
            AsyncLogger.info("      Cidades monitoradas no coletor: " + coletorService.getCidades());

            int porta = PORTA_PADRAO;
            String portaEnv = System.getenv("PORT");
            if (portaEnv != null && !portaEnv.isBlank()) {
                try {
                    porta = Integer.parseInt(portaEnv);
                } catch (NumberFormatException ignored) {}
            }

            // Resolve o diretório do frontend (pasta 'frontend' ao lado do pom.xml / JAR)
            Path frontendDir = Paths.get(System.getProperty("frontend.dir",
                    Paths.get("").toAbsolutePath().resolve("frontend").toString()));
            AsyncLogger.info("      Diretório do frontend: " + frontendDir);

            HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            MedicaoApiHandler apiHandler = new MedicaoApiHandler(monitoramentoService);
            HealthHandler healthHandler = new HealthHandler();
            WeatherProxyHandler proxyHandler = new WeatherProxyHandler();
            FrontendStaticHandler frontendHandler = new FrontendStaticHandler(frontendDir);

            // Rota raiz — serve o frontend diretamente
            server.createContext("/", exchange -> {
                HttpRequest req = new HttpRequestImpl(exchange);
                HttpResponseSender send = new HttpResponseSenderImpl(exchange);
                String path = req.getPath();

                // Serve index.html na raiz
                if ("/".equals(path) || "/index.html".equals(path)) {
                    frontendHandler.handle(exchange);
                    return;
                }

                // Documenta a API em /api
                if ("/api".equals(path)) {
                    String infoApi = """
                {
                  "servico": "API Estação de Monitoramento Ambiental (APS - RESTful v1)",
                  "versao": "1.0.0",
                  "idioma": "pt-BR",
                  "status": "online",
                  "fonteMeteorologica": "API Real do OpenWeatherMap (com Cache em Memória e Novas Tentativas Automáticas)",
                  "banco": "MySQL (Hibernate 6 + HikariCP Connection Pooling)",
                  "otimizacoes": [
                    "Pool de Conexões (HikariCP com limite de 20 conexões e tempo limite de inatividade)",
                    "Gerenciador de Conexões (Controle centralizado de transações ACID e métricas)",
                    "Cache em Memória (Gerenciador com tempo de expiração TTL para cidades e estatísticas)",
                    "Consultas Agregadas Otimizadas (Agregação SQL direta no banco sem consultas N+1)",
                    "Compactação de Resposta (GZIP automático para clientes compatíveis)",
                    "Registro Assíncrono de Logs (Fila de eventos desacoplada das threads de requisição)",
                    "Detalhamento Padronizado de Erros (RFC 7807)"
                  ],
                  "endpoints": [
                    "GET /health (Monitoramento de integridade do sistema, pool HikariCP e cache)",
                    "GET /api/v1/health (Verificação de saúde da aplicação versionada)",
                    "GET /api/v1/medicoes (Listagem paginada com filtros por cidade, temperatura e ordenação)",
                    "GET /api/v1/medicoes/{id} (Consulta de medição por identificador com links de navegação)",
                    "GET /api/v1/medicoes/atual?cidade={cidade} (Consulta de clima em tempo real com tolerância a falhas)",
                    "GET /api/v1/medicoes/estatisticas (Estatísticas climáticas consolidadas com cache)",
                    "GET /api/v1/cidades (Listagem de cidades cadastradas com cache em memória)",
                    "GET /api/v1/cidades/{cidade}/medicoes (Medições filtradas por município)",
                    "GET /api/v1/cidades/{cidade}/estatisticas (Estatísticas climáticas específicas do município)",
                    "GET /api/v1/estacoes/{estacaoId}/medicoes (Medições filtradas por estação meteorológica)",
                    "POST /api/v1/medicoes (Cadastro de medição com validação física de dados)",
                    "POST /api/v1/sincronizacoes (Sincronização em lote com inserções otimizadas)",
                    "POST /api/v1/coletor/cidades (Inclusão de localização na rotina de coleta automática)",
                    "POST /api/v1/coletor/execucoes (Disparo imediato do ciclo de coleta meteorológica)",
                    "DELETE /api/v1/medicoes/{id} (Exclusão de medição por identificador e limpeza de cache)",
                    "DELETE /api/v1/medicoes?cidade={cidade} (Exclusão de histórico por município e limpeza de cache)"
                  ]
                }
                """;
                    send.sucesso(infoApi);
                    return;
                }

                // Qualquer outra rota desconhecida
                send.naoEncontrado("Rota não encontrada");
            });

            // Endpoints de Saúde (Health Checks)
            server.createContext("/health", healthHandler);
            server.createContext("/api/v1/health", healthHandler);

            // Endpoints Modernos RESTful v1 (Clean Naming, Versioning, Cross-Resources)
            server.createContext("/api/v1/medicoes", apiHandler);
            server.createContext("/api/v1/cidades", apiHandler);
            server.createContext("/api/v1/estacoes", apiHandler);
            server.createContext("/api/v1/sincronizacoes", apiHandler);
            server.createContext("/api/v1/coletor", apiHandler);

            // Endpoints Legados Mantidos para Retrocompatibilidade
            server.createContext("/api/medicoes", apiHandler);
            server.createContext("/api/coletor", apiHandler);

            // ─── INTEGRAÇÃO FRONTEND ────────────────────────────────────────────
            // Proxy reverso: repassa chamadas OWM protegendo a API key no servidor
            server.createContext("/proxy", proxyHandler);

            // Servidor de arquivos estáticos: serve o frontend em /frontend/*
            server.createContext("/frontend", frontendHandler);
            // ────────────────────────────────────────────────────────────────────

            server.start();
            AsyncLogger.info("[4/4] Servidor HTTP RESTful v1 iniciado com sucesso!");
            AsyncLogger.info("------------------------------------------------------------------");
            AsyncLogger.info("  Base URL:          http://localhost:" + porta + "/");
            AsyncLogger.info("  Frontend:          http://localhost:" + porta + "/frontend/");
            AsyncLogger.info("  Health:            http://localhost:" + porta + "/health");
            AsyncLogger.info("  Proxy OWM:         http://localhost:" + porta + "/proxy/weather?lat=-23.5&lon=-46.6");
            AsyncLogger.info("  GET Paginado:      http://localhost:" + porta + "/api/v1/medicoes?page=1&pageSize=10");
            AsyncLogger.info("  GET Estatisticas:  http://localhost:" + porta + "/api/v1/medicoes/estatisticas");
            AsyncLogger.info("  GET Cidades:       http://localhost:" + porta + "/api/v1/cidades");
            AsyncLogger.info("------------------------------------------------------------------");
            AsyncLogger.info("Pronto para receber requisições com HikariCP, Cache e GZIP ativos. CTRL+C para sair.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                AsyncLogger.info("\n[!] Encerrando aplicação graciosamente...");
                coletorService.parar();
                server.stop(1);
                connectionManager.shutdown();
                AsyncLogger.info("[+] Aplicação finalizada com sucesso.");
                AsyncLogger.shutdown();
            }));

        } catch (IOException e) {
            AsyncLogger.error("[-] Erro ao iniciar o servidor HTTP: " + e.getMessage(), e);
        } catch (Throwable t) {
            AsyncLogger.error("[-] Falha crítica na inicialização: " + t.getMessage(), t);
            t.printStackTrace();
        }
    }
}
