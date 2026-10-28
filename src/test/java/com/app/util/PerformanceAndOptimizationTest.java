package com.app.util;

import com.app.util.cache.CacheManager;
import com.app.util.logging.AsyncLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceAndOptimizationTest {

    @Test
    @DisplayName("CacheManager: Deve armazenar, recuperar e invalidar dados por prefixo")
    void deveGerenciarCacheComTtlEInvalidacao() {
        CacheManager cache = CacheManager.getInstance();
        cache.clear();

        cache.put("cidades:lista", List.of("Curitiba", "Tokyo"), Duration.ofMinutes(1));
        Optional<List<String>> cidades = cache.get("cidades:lista");

        assertTrue(cidades.isPresent(), "Item deve estar presente no cache");
        assertEquals(2, cidades.get().size());
        assertTrue(cache.getHits() > 0);

        // Invalidação por prefixo
        cache.invalidatePrefix("cidades");
        Optional<List<String>> posInvalidacao = cache.get("cidades:lista");
        assertFalse(posInvalidacao.isPresent(), "Item deve ter sido removido após invalidar prefixo");
    }

    @Test
    @DisplayName("CacheManager: Deve expirar itens após o tempo de vida (TTL)")
    void deveExpirarItensComTtlCurto() throws InterruptedException {
        CacheManager cache = CacheManager.getInstance();
        cache.put("teste:expiracao", "valorTemporario", Duration.ofMillis(50));

        Optional<String> antes = cache.get("teste:expiracao");
        assertTrue(antes.isPresent());

        Thread.sleep(70);

        Optional<String> depois = cache.get("teste:expiracao");
        assertFalse(depois.isPresent(), "Item expirado não deve ser retornado pelo cache");
    }

    @Test
    @DisplayName("MedicaoAmbiental JPA: Deve respeitar o contrato de equals e hashCode")
    void deveRespeitarContratoEqualsEHashCode() {
        com.app.model.MedicaoAmbiental m1 = new com.app.model.MedicaoAmbiental();
        m1.setId(1L);
        com.app.model.MedicaoAmbiental m2 = new com.app.model.MedicaoAmbiental();
        m2.setId(1L);
        com.app.model.MedicaoAmbiental m3 = new com.app.model.MedicaoAmbiental();
        m3.setId(2L);
        com.app.model.MedicaoAmbiental transiente1 = new com.app.model.MedicaoAmbiental();
        com.app.model.MedicaoAmbiental transiente2 = new com.app.model.MedicaoAmbiental();

        assertEquals(m1, m2, "Entidades com o mesmo ID devem ser iguais");
        assertNotEquals(m1, m3, "Entidades com IDs diferentes não devem ser iguais");
        assertEquals(m1.hashCode(), m2.hashCode(), "HashCodes devem ser consistentes");
        assertNotEquals(transiente1, transiente2, "Entidades transientes distintas não devem ser iguais");
        assertEquals(transiente1, transiente1, "Entidade transiente é igual a si mesma");
    }

    @Test
    @DisplayName("AsyncLogger: Deve enfileirar mensagens assíncronas sem exceções")
    void deveRegistrarLogsAssincronosSemFalhas() {
        assertDoesNotThrow(() -> {
            AsyncLogger.info("Teste de log informativo assíncrono");
            AsyncLogger.warn("Teste de log de aviso assíncrono");
            AsyncLogger.debug("Teste de log de depuração assíncrono");
            AsyncLogger.error("Teste de log de erro assíncrono", new RuntimeException("Erro simulado"));
        });
    }
}
