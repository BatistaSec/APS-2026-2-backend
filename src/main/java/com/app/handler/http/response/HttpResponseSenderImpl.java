package com.app.handler.http.response;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class HttpResponseSenderImpl implements HttpResponseSender {

    private final HttpExchange exchange;

    public HttpResponseSenderImpl(HttpExchange exchange) {
        this.exchange = exchange;
    }

    private void enviar(int statusCode, String json) throws IOException {
        byte[] bytes = (json != null && !json.isBlank()) ? json.getBytes(StandardCharsets.UTF_8) : new byte[0];
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if (statusCode == 204 || bytes.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    @Override
    public void sucesso(String json) throws IOException {
        enviar(200, json);
    }

    @Override
    public void criado(String json) throws IOException {
        enviar(201, json);
    }

    @Override
    public void badRequest(String mensagem) throws IOException {
        enviar(400, "{\"erro\":\"" + sanitizar(mensagem) + "\"}");
    }

    @Override
    public void naoEncontrado(String mensagem) throws IOException {
        enviar(404, "{\"mensagem\":\"" + sanitizar(mensagem) + "\"}");
    }

    @Override
    public void metodoNaoPermitido() throws IOException {
        enviar(405, "{\"erro\":\"Método não permitido\"}");
    }

    @Override
    public void erroInterno(String mensagem) throws IOException {
        enviar(500, "{\"erro\":\"" + sanitizar(mensagem) + "\"}");
    }

    @Override
    public void noContent() throws IOException {
        enviar(204, "");
    }

    private String sanitizar(String msg) {
        return msg != null ? msg.replace("\"", "\\\"").replace("\n", " ") : "Ocorreu um erro";
    }
}
