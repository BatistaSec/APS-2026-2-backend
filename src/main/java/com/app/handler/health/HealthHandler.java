package com.app.handler.health;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
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
 * Expõe métricas internas (pool HikariCP, cache, JVM, transações).
 */
public class HealthHandler implements HttpHandler {

    private final DatabaseConnectionManager dbManager;

    public HealthHandler() {
        this(DatabaseConnectionManager.getInstance());
    }

    public HealthHandler(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpRequest req  = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if (!"GET".equalsIgnoreCase(req.getMethod())) {
            send.metodoNaoPermitido();
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
                "\"connectionPooling\":\"Hibernate (DriverManager)\"," +
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
