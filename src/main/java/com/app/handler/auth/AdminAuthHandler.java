package com.app.handler.auth;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.app.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * POST /auth/login
 *
 * Body: {"usuario": "admin", "senha": "admin"}
 * Retorna: {"token": "...", "perfil": "admin"} com HTTP 200
 *      ou: 401 Unauthorized se as credenciais forem inválidas.
 *
 * O token retornado é o mesmo valor configurado em SecurityService
 * (variável ADMIN_TOKEN ou fallback padrão).
 * Toda a validação de credenciais fica aqui no backend — o frontend
 * apenas armazena e reenvia o token como Bearer.
 */
public class AdminAuthHandler implements HttpHandler {

    // Credenciais do administrador — hardcoded para projeto acadêmico.
    // Em produção, use hash bcrypt e banco de dados.
    static final String ADMIN_USER  = "admin";
    static final String ADMIN_SENHA = "admin";

    /** Token que será exigido nas rotas protegidas. */
    public static String getAdminToken() {
        String env = System.getenv("ADMIN_TOKEN");
        if (env != null && !env.isBlank()) return env.trim();
        // Fallback fixo para projeto acadêmico
        return "aps-admin-token-2024";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpRequest  req  = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) { send.noContent(); return; }

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            send.metodoNaoPermitido();
            return;
        }

        String corpo = req.getBody();
        String usuario = JsonUtil.extrairTexto(corpo, "usuario");
        String senha   = JsonUtil.extrairTexto(corpo, "senha");

        if (ADMIN_USER.equals(usuario) && ADMIN_SENHA.equals(senha)) {
            String token = getAdminToken();
            String json = String.format(
                    "{\"token\":\"%s\",\"perfil\":\"admin\",\"mensagem\":\"Autenticado com sucesso\"}",
                    token
            );
            send.sucesso(json);
        } else {
            send.unauthorized("Credenciais inválidas. Verifique usuário e senha.");
        }
    }
}
