package com.app.handler.health;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.app.handler.security.SecurityService;
import com.app.util.cache.CacheManager;
import com.app.util.database.DatabaseConnectionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * GET /health e GET /api/v1/health
 *
 * Rota de administrador: exige Bearer token de admin.
 * Expõe métricas internas (pool HikariCP, cache, JVM, transações).
 */
public class HealthHandler implements HttpHandler {

    private final DatabaseConnectionManager dbManager;
    private final SecurityService securityService;

    public HealthHandler() {
        this(DatabaseConnectionManager.getInstance(), new SecurityService());
    }

    public HealthHandler(DatabaseConnectionManager dbManager) {
        this(dbManager, new SecurityService());
    }

    public HealthHandler(DatabaseConnectionManager dbManager, SecurityService securityService) {
        this.dbManager       = dbManager;
        this.securityService = securityService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpRequest req  = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            send.metodoNaoPermitido();
            return;
        }

        // Apenas administradores podem ver métricas internas do sistema
        if (!securityService.autorizarOperacaoEscrita(req)) {
            send.unauthorized("Acesso restrito: rota de administrador. Envie Authorization: Bearer <token>.");
            return;
        }

        boolean dbOk = dbManager.isHealthy();
        CacheManager cache = CacheManager.getInstance();
        Runtime rt = Runtime.getRuntime();
        long memTotal = rt.totalMemory() / (1024 * 1024);
        long memLivre = rt.freeMemory()  / (1024 * 1024);
        long memUsada = memTotal - memLivre;

        String statusGeral = dbOk ? "UP" : "DOWN";
        String statusDb    = dbOk ? "UP" : "DOWN";
        String timestamp   = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String json = String.format(Locale.US,
                "{" +
                "\"status\":\"%s\"," +
                "\"versao\":\"1.0.0\"," +
                "\"componentes\":{" +
                "\"bancoDeDados\":\"%s\"," +
                "\"connectionPooling\":\"HikariCP (Ativo)\"," +
                "\"coletorSegundoPlano\":\"UP\"," +
                "\"openWeather\":\"CONFIGURADO\"" +
                "}," +
                "\"metricasCache\":{" +
                "\"itensEmCache\":%d," +
                "\"hits\":%d," +
                "\"misses\":%d," +
                "\"hitRatePorcentagem\":%.2f" +
                "}," +
                "\"metricasBanco\":{" +
                "\"transacoesSucesso\":%d," +
                "\"transacoesFalha\":%d" +
                "}," +
                "\"sistema\":{" +
                "\"jvm\":\"Java 21\"," +
                "\"memoriaUsadaMb\":%d," +
                "\"memoriaTotalMb\":%d" +
                "}," +
                "\"timestamp\":\"%s\"" +
                "}",
                statusGeral, statusDb,
                cache.size(), cache.getHits(), cache.getMisses(), cache.getHitRate(),
                dbManager.getTransacoesSucesso(), dbManager.getTransacoesFalha(),
                memUsada, memTotal, timestamp
        );

        if (dbOk) {
            send.sucesso(json);
        } else {
            send.erroInterno("Conexão com o banco de dados indisponível no momento.");
        }
    }
}
