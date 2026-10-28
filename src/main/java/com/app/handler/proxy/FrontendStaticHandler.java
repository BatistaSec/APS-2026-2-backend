package com.app.handler.proxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

/**
 * Serve os arquivos estáticos do frontend (HTML, CSS, JS) diretamente
 * pelo servidor Java, eliminando a necessidade de um servidor web separado.
 *
 * Rota: GET /frontend/* -> mapeia para o diretório 'frontend/' relativo ao JAR.
 * Rota: GET /           -> redireciona para /frontend/
 */
public class FrontendStaticHandler implements HttpHandler {

    private static final Map<String, String> MIME_TYPES = Map.of(
            ".html", "text/html; charset=UTF-8",
            ".css",  "text/css; charset=UTF-8",
            ".js",   "application/javascript; charset=UTF-8",
            ".json", "application/json; charset=UTF-8",
            ".svg",  "image/svg+xml",
            ".ico",  "image/x-icon",
            ".png",  "image/png",
            ".jpg",  "image/jpeg",
            ".webp", "image/webp"
    );

    private final Path frontendDir;

    public FrontendStaticHandler(Path frontendDir) {
        this.frontendDir = frontendDir.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equals(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();

        // Rota /admin e /admin/ → serve admin.html
        if (requestPath.equals("/admin") || requestPath.equals("/admin/")) {
            servirArquivo(exchange, frontendDir.resolve("admin.html"));
            return;
        }

        // Redireciona /frontend ou /frontend/ → index.html
        if (requestPath.equals("/frontend") || requestPath.equals("/frontend/")) {
            requestPath = "/frontend/index.html";
        }

        // Remove o prefixo /frontend/
        String relativePath = requestPath.replaceFirst("^/frontend/", "");
        if (relativePath.isBlank()) relativePath = "index.html";

        // Segurança: bloqueia path traversal
        Path filePath = frontendDir.resolve(relativePath).normalize();
        if (!filePath.startsWith(frontendDir)) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            Path indexFallback = frontendDir.resolve("index.html");
            if (Files.exists(indexFallback)) {
                filePath = indexFallback;
            } else {
                byte[] msg = "Arquivo não encontrado.".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }
        }

        servirArquivo(exchange, filePath);
    }

    private void servirArquivo(HttpExchange exchange, Path filePath) throws IOException {
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            byte[] msg = "Arquivo não encontrado.".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
            return;
        }
        String ext = getExtension(filePath.getFileName().toString());
        String mimeType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        byte[] content = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }
}
