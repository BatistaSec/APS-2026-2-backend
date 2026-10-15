package com.app;

import com.app.handler.MedicaoApiHandler;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.app.service.MonitoramentoService;
import com.app.service.coleta.ColetorAgendadoService;
import com.app.util.HibernateUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORTA_PADRAO = 8080;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("     SISTEMA DE ESTACAO DE MONITORAMENTO AMBIENTAL (APS)          ");
        System.out.println("     Backend Java 21 + Hibernate + MySQL + OpenWeatherMap Real    ");
        System.out.println("     Arquitetura SOLID (SRP, OCP, LSP, ISP, DIP)                 ");
        System.out.println("==================================================================");

        try {
            System.out.println("[1/4] Conectando ao MySQL e sincronizando tabelas via Hibernate...");
            HibernateUtil.getSessionFactory();

            System.out.println("[2/4] Inicializando camadas DAO (SRP) e Serviços Especializados...");
            MonitoramentoService monitoramentoService = new MonitoramentoService();

            System.out.println("[3/4] Inicializando Coletor Agendado (ciclos a cada 10s)...");
            ColetorAgendadoService coletorService = monitoramentoService.getColetorService();
            coletorService.iniciar();
            System.out.println("      Cidades monitoradas no coletor: " + coletorService.getCidades());

            int porta = PORTA_PADRAO;
            String portaEnv = System.getenv("PORT");
            if (portaEnv != null && !portaEnv.isBlank()) {
                try {
                    porta = Integer.parseInt(portaEnv);
                } catch (NumberFormatException ignored) {}
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            MedicaoApiHandler apiHandler = new MedicaoApiHandler(monitoramentoService);

            // Rota raiz com documentação dos endpoints
            server.createContext("/", exchange -> {
                HttpRequest req = new HttpRequestImpl(exchange);
                HttpResponseSender send = new HttpResponseSenderImpl(exchange);

                if (!"/".equals(req.getPath())) {
                    send.naoEncontrado("Rota não encontrada");
                    return;
                }
                String infoApi = """
                {
                  "servico": "API Estação de Monitoramento Ambiental (APS - SOLID)",
                  "status": "online",
                  "fonteMeteorologica": "OpenWeatherMap API Real (Qualquer Cidade)",
                  "banco": "MySQL (Hibernate DAO)",
                  "intervaloColetaSegundoPlano": "10s",
                  "arquitetura": "SOLID (SRP, OCP, LSP, ISP, DIP)",
                  "endpoints": [
                    "POST /api/medicoes (enviar {'cidade':'Curitiba'} para consultar OpenWeatherMap e salvar)",
                    "POST /api/medicoes?cidade={qualquer_cidade} (mesmo efeito via URL)",
                    "POST /api/medicoes/sincronizar (lote de cidades: {'cidades':['Tokyo','Paris','Curitiba']})",
                    "POST /api/coletor/cidades (adicionar cidade ao coletor de 10s: {'cidade':'Rome'})",
                    "POST /api/coletor/executar (disparar ciclo de coleta imediatamente)",
                    "GET /api/medicoes (listar últimas medições - suporta ?cidade={cidade}&limite={N})",
                    "GET /api/medicoes/atual?cidade={qualquer_cidade} (clima ao vivo via OpenWeatherMap)",
                    "GET /api/medicoes/cidades (listar cidades com dados no MySQL)",
                    "GET /api/medicoes/estatisticas?cidade={qualquer_cidade} (médias, picos e amplitudes)",
                    "GET /api/medicoes/{id} (buscar medição por ID)",
                    "DELETE /api/medicoes/{id} (remover medição por ID)",
                    "DELETE /api/medicoes?cidade={qualquer_cidade} (remover histórico de uma cidade)"
                  ]
                }
                """;
                send.sucesso(infoApi);
            });

            server.createContext("/api/medicoes", apiHandler);
            server.createContext("/api/coletor", apiHandler);

            server.start();
            System.out.println("[4/4] Servidor HTTP REST iniciado com sucesso!");
            System.out.println("------------------------------------------------------------------");
            System.out.printf("  Base URL:          http://localhost:%d/%n", porta);
            System.out.printf("  GET Listar:        http://localhost:%d/api/medicoes?cidade=Curitiba%n", porta);
            System.out.printf("  GET Ao Vivo:       http://localhost:%d/api/medicoes/atual?cidade=Tokyo%n", porta);
            System.out.printf("  GET Cidades:       http://localhost:%d/api/medicoes/cidades%n", porta);
            System.out.printf("  GET Estatisticas:  http://localhost:%d/api/medicoes/estatisticas?cidade=Curitiba%n", porta);
            System.out.printf("  POST Inserir:      http://localhost:%d/api/medicoes (Body: {\"cidade\":\"Curitiba\"})%n", porta);
            System.out.printf("  POST Sincronizar:  http://localhost:%d/api/medicoes/sincronizar%n", porta);
            System.out.printf("  DELETE Cidade:     http://localhost:%d/api/medicoes?cidade=Miami%n", porta);
            System.out.println("------------------------------------------------------------------");
            System.out.println("Pronto para receber requisições. Pressione CTRL+C para sair.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[!] Encerrando aplicação graciosamente...");
                coletorService.parar();
                server.stop(1);
                HibernateUtil.shutdown();
                System.out.println("[+] Aplicação finalizada.");
            }));

        } catch (IOException e) {
            System.err.println("[-] Erro ao iniciar o servidor HTTP: " + e.getMessage());
        } catch (Throwable t) {
            System.err.println("[-] Falha crítica na inicialização: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
