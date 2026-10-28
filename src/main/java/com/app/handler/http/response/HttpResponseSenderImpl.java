package com.app.handler.http.response;

import com.app.model.dto.response.ProblemDetails;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class HttpResponseSenderImpl implements HttpResponseSender {

    private final HttpExchange exchange;

    public HttpResponseSenderImpl(HttpExchange exchange) {
        this.exchange = exchange;
    }

    private void enviar(int statusCode, String json) throws IOException {
        byte[] bytes = (json != null && !json.isBlank()) ? json.getBytes(StandardCharsets.UTF_8) : new byte[0];

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Language", "pt-BR");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Forwarded-For, Accept-Encoding");

        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");

        if (statusCode == 204 || bytes.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
            return;
        }

        String acceptEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");
        boolean suportaGzip = acceptEncoding != null && acceptEncoding.toLowerCase().contains("gzip");

        if (suportaGzip && bytes.length > 256) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(bytes);
            }
            byte[] compressed = baos.toByteArray();
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.getResponseHeaders().set("Vary", "Accept-Encoding");
            exchange.sendResponseHeaders(statusCode, compressed.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(compressed);
            }
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
        String path = exchange.getRequestURI().getPath();
        String detalhe = (mensagem != null && !mensagem.isBlank())
                ? mensagem
                : com.app.util.i18n.I18n.get("erro.bad_request.detalhe_padrao");
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/validacao",
                com.app.util.i18n.I18n.get("erro.bad_request.titulo"),
                400,
                detalhe,
                path
        );
        enviar(400, problem.toJson());
    }

    @Override
    public void naoEncontrado(String mensagem) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String detalhe = (mensagem != null && !mensagem.isBlank())
                ? mensagem
                : com.app.util.i18n.I18n.get("erro.nao_encontrado.detalhe_padrao");
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/nao-encontrado",
                com.app.util.i18n.I18n.get("erro.nao_encontrado.titulo"),
                404,
                detalhe,
                path
        );
        enviar(404, problem.toJson());
    }

    @Override
    public void metodoNaoPermitido() throws IOException {
        String path = exchange.getRequestURI().getPath();
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/metodo-nao-permitido",
                com.app.util.i18n.I18n.get("erro.metodo_nao_permitido.titulo"),
                405,
                com.app.util.i18n.I18n.get("erro.metodo_nao_permitido.detalhe", exchange.getRequestMethod()),
                path
        );
        enviar(405, problem.toJson());
    }

    @Override
    public void erroInterno(String mensagem) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String detalhe = (mensagem != null && !mensagem.isBlank())
                ? mensagem
                : com.app.util.i18n.I18n.get("erro.interno.detalhe_padrao");
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/servidor",
                com.app.util.i18n.I18n.get("erro.interno.titulo"),
                500,
                detalhe,
                path
        );
        enviar(500, problem.toJson());
    }

    @Override
    public void unauthorized(String mensagem) throws IOException {
        String path = exchange.getRequestURI().getPath();
        exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer realm=\"APS-API\"");
        String detalhe = (mensagem != null && !mensagem.isBlank())
                ? mensagem
                : com.app.util.i18n.I18n.get("erro.nao_autorizado.detalhe_padrao");
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/nao-autorizado",
                com.app.util.i18n.I18n.get("erro.nao_autorizado.titulo"),
                401,
                detalhe,
                path
        );
        enviar(401, problem.toJson());
    }

    @Override
    public void tooManyRequests(String mensagem, long retryAfterSeconds) throws IOException {
        String path = exchange.getRequestURI().getPath();
        exchange.getResponseHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        String detalhe = (mensagem != null && !mensagem.isBlank())
                ? mensagem
                : com.app.util.i18n.I18n.get("erro.limite_requisicoes.detalhe", "cliente", retryAfterSeconds);
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/limite-requisicoes",
                com.app.util.i18n.I18n.get("erro.limite_requisicoes.titulo"),
                429,
                detalhe,
                path
        );
        enviar(429, problem.toJson());
    }

    @Override
    public void problema(ProblemDetails problem) throws IOException {
        enviar(problem.status(), problem.toJson());
    }

    @Override
    public void noContent() throws IOException {
        enviar(204, "");
    }

    @Override
    public void setHeader(String nome, String valor) {
        if (nome != null && valor != null) {
            exchange.getResponseHeaders().set(nome, valor);
        }
    }
}
