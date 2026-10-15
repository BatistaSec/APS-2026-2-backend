package com.app.handler.http.request;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;


public class HttpRequestImpl implements HttpRequest {

    private final HttpExchange exchange;
    private String cachedBody;

    public HttpRequestImpl(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public String getMethod() {
        return exchange.getRequestMethod().toUpperCase();
    }

    @Override
    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    @Override
    public String getQueryParam(String nome) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) return null;
        for (String param : query.split("&")) {
            String[] partes = param.split("=", 2);
            if (partes.length == 2 && partes[0].equalsIgnoreCase(nome)) {
                try {
                    return URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return partes[1];
                }
            }
        }
        return null;
    }

    @Override
    public int getQueryParamInt(String nome, int padrao) {
        String val = getQueryParam(nome);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return padrao;
    }

    @Override
    public String getBody() {
        if (cachedBody == null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                cachedBody = reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                cachedBody = "";
            }
        }
        return cachedBody;
    }

    @Override
    public Long getIdFromPath() {
        String path = getPath();
        String[] partes = path.split("/");
        if (partes.length >= 4) {
            try {
                return Long.parseLong(partes[3]);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
