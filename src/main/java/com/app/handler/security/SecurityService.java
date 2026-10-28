package com.app.handler.security;

import com.app.handler.auth.AdminAuthHandler;
import com.app.handler.http.request.HttpRequest;

/**
 * Verifica se a requisição carrega o Bearer token de administrador.
 *
 * O token válido é obtido de AdminAuthHandler.getAdminToken() — mesma
 * fonte usada pelo endpoint POST /auth/login. Toda a lógica de autorização
 * fica aqui no backend; o frontend apenas encaminha o token que recebeu.
 */
public class SecurityService {

    private final String requiredToken;

    public SecurityService() {
        // Sempre exige autenticação — token vem do AdminAuthHandler
        this(AdminAuthHandler.getAdminToken());
    }

    public SecurityService(String requiredToken) {
        this.requiredToken = (requiredToken != null && !requiredToken.isBlank()) ? requiredToken.trim() : null;
    }

    /**
     * Retorna true se o request contém o header Authorization: Bearer <token>
     * com o valor correto do token de administrador.
     */
    public boolean autorizarOperacaoEscrita(HttpRequest req) {
        if (requiredToken == null) {
            // Sem token configurado → nenhuma operação de escrita é permitida
            return false;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return false;
        }

        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return requiredToken.equals(token);
        }

        return false;
    }
}
