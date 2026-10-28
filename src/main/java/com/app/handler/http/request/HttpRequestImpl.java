package com.app.handler.http.request;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
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
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return padrao;
    }

    @Override
    public Double getQueryParamDouble(String nome, Double padrao) {
        String val = getQueryParam(nome);
        if (val != null) {
            try {
                return Double.parseDouble(val.trim());
            } catch (NumberFormatException ignored) {}
        }
        return padrao;
    }

    @Override
    public Boolean getQueryParamBoolean(String nome, Boolean padrao) {
        String val = getQueryParam(nome);
        if (val != null) {
            return Boolean.parseBoolean(val.trim());
        }
        return padrao;
    }

    @Override
    public String getHeader(String nome) {
        if (nome == null) return null;
        return exchange.getRequestHeaders().getFirst(nome);
    }

    @Override
    public String getClientIp() {

        String xForwardedFor = getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "127.0.0.1";
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
        for (int i = partes.length - 1; i >= 0; i--) {
            String segmento = partes[i];
            if (!segmento.isBlank()) {
                try {
                    return Long.parseLong(segmento);
                } catch (NumberFormatException ignored) {
                    // Não é numérico, continua verificando
                    break;
                }
            }
        }
        return null;
    }
}
