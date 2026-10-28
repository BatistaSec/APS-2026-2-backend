package com.app.handler.security;

import com.app.handler.http.request.HttpRequest;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.MedicaoResponse;
import com.app.model.dto.response.PaginatedResponse;
import com.app.model.dto.response.ProblemDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndReliabilityTest {

    @Test
    @DisplayName("RateLimiter: Deve bloquear requisições quando ultrapassar o limite por minuto")
    void deveLimitarTaxaDeRequisicoesPorIp() {
        RateLimiter limiter = new RateLimiter(5);
        String ip = "192.168.1.100";

        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(ip), "Requisição " + (i + 1) + " deveria ser permitida");
        }

        assertFalse(limiter.tryAcquire(ip), "A 6ª requisição deve ser bloqueada pelo Rate Limiter");
        assertEquals(0, limiter.getRemaining(ip));
        assertTrue(limiter.getResetSeconds(ip) > 0);
    }

    @Test
    @DisplayName("ProblemDetails (RFC 7807): Deve gerar JSON formatado semântico para erros")
    void deveGerarProblemDetailsJsonValido() {
        ProblemDetails problem = ProblemDetails.of(
                "https://api.aps-clima.com/erros/validacao",
                "Erro de Validação",
                400,
                "Temperatura fora do limite aceitável.",
                "/api/v1/medicoes"
        );

        String json = problem.toJson();
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("\"title\":\"Erro de Validação\""));
        assertTrue(json.contains("\"detail\":\"Temperatura fora do limite aceitável.\""));
        assertTrue(json.contains("\"instance\":\"/api/v1/medicoes\""));
    }

    @Test
    @DisplayName("PaginatedResponse: Deve calcular páginas e gerar metadados de paginação")
    void deveGerarPaginacaoComMetadados() {
        MedicaoAmbiental m = new MedicaoAmbiental("EST-01", "Curitiba", LocalDateTime.now(), 20.0, 50.0, false, 10.0, 1013.0, "N", 3.0, 20.0);
        MedicaoResponse dto = MedicaoResponse.fromEntity(m);

        PaginatedResponse pag = PaginatedResponse.of(List.of(dto), 1, 10, 25);
        assertEquals(1, pag.page());
        assertEquals(10, pag.pageSize());
        assertEquals(25, pag.totalItems());
        assertEquals(3, pag.totalPages());
        assertTrue(pag.hasNext());
        assertFalse(pag.hasPrevious());

        String json = pag.toJson();
        assertTrue(json.contains("\"pagination\":{"));
        assertTrue(json.contains("\"totalPages\":3"));
        assertTrue(json.contains("\"totalItems\":25"));
    }

    @Test
    @DisplayName("Cross-Resource References: MedicaoResponse deve conter _links no JSON")
    void deveConterLinksEmMedicaoResponse() {
        MedicaoAmbiental m = new MedicaoAmbiental("EST-01", "Curitiba", LocalDateTime.now(), 20.0, 50.0, false, 10.0, 1013.0, "N", 3.0, 20.0);
        m.setId(42L);
        MedicaoResponse dto = MedicaoResponse.fromEntity(m);

        String json = dto.toJson();
        assertTrue(json.contains("\"_links\":{"), "JSON deve conter o bloco _links");
        assertTrue(json.contains("\"self\":\"/api/v1/medicoes/42\""));
        assertTrue(json.contains("\"cidade\":\"/api/v1/cidades/Curitiba/medicoes\""));
        assertTrue(json.contains("\"estatisticas\":\"/api/v1/cidades/Curitiba/estatisticas\""));
    }

    @Test
    @DisplayName("SecurityService: Deve validar Bearer Token quando configurado")
    void deveValidarBearerToken() {
        SecurityService secWithToken = new SecurityService("meu-token-secreto-2026");

        HttpRequest reqSemToken = criarRequestComHeader(null);
        assertFalse(secWithToken.autorizarOperacaoEscrita(reqSemToken), "Sem header deve negar");

        HttpRequest reqTokenInvalido = criarRequestComHeader("Bearer token-errado");
        assertFalse(secWithToken.autorizarOperacaoEscrita(reqTokenInvalido), "Token errado deve negar");

        HttpRequest reqTokenCorreto = criarRequestComHeader("Bearer meu-token-secreto-2026");
        assertTrue(secWithToken.autorizarOperacaoEscrita(reqTokenCorreto), "Token correto deve autorizar");
    }

    private HttpRequest criarRequestComHeader(String authHeader) {
        return new HttpRequest() {
            @Override public String getMethod() { return "POST"; }
            @Override public String getPath() { return "/api/v1/medicoes"; }
            @Override public String getQueryParam(String nome) { return null; }
            @Override public int getQueryParamInt(String nome, int padrao) { return padrao; }
            @Override public Double getQueryParamDouble(String nome, Double padrao) { return padrao; }
            @Override public Boolean getQueryParamBoolean(String nome, Boolean padrao) { return padrao; }
            @Override public String getHeader(String nome) { return "Authorization".equalsIgnoreCase(nome) ? authHeader : null; }
            @Override public String getClientIp() { return "127.0.0.1"; }
            @Override public String getBody() { return "{}"; }
            @Override public Long getIdFromPath() { return null; }
        };
    }
}
