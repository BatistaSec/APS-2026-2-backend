package com.app.util.database;

import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.handler.health.HealthHandler;
import com.app.model.MedicaoAmbiental;
import com.app.util.cache.CacheManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionManagerTest {

    private DatabaseConnectionManager dbManager;
    private AtomicBoolean commitChamado;
    private AtomicBoolean rollbackChamado;
    private AtomicBoolean sessionFechada;
    private boolean lancarErroNaOperacao;

    @BeforeEach
    void setUp() {
        commitChamado = new AtomicBoolean(false);
        rollbackChamado = new AtomicBoolean(false);
        sessionFechada = new AtomicBoolean(false);
        lancarErroNaOperacao = false;

        Transaction mockTx = (Transaction) Proxy.newProxyInstance(
                Transaction.class.getClassLoader(),
                new Class<?>[]{Transaction.class},
                (proxy, method, args) -> {
                    if ("commit".equals(method.getName())) {
                        commitChamado.set(true);
                        return null;
                    }
                    if ("rollback".equals(method.getName())) {
                        rollbackChamado.set(true);
                        return null;
                    }
                    if ("isActive".equals(method.getName())) {
                        return true;
                    }
                    return null;
                }
        );

        Session mockSession = (Session) Proxy.newProxyInstance(
                Session.class.getClassLoader(),
                new Class<?>[]{Session.class},
                (proxy, method, args) -> {
                    if ("beginTransaction".equals(method.getName())) {
                        return mockTx;
                    }
                    if ("close".equals(method.getName())) {
                        sessionFechada.set(true);
                        return null;
                    }
                    if ("persist".equals(method.getName())) {
                        if (lancarErroNaOperacao) {
                            throw new RuntimeException("Erro forçado de banco de dados para teste de rollback");
                        }
                        return null;
                    }
                    if ("isOpen".equals(method.getName())) {
                        return true;
                    }
                    return null;
                }
        );

        SessionFactory mockSessionFactory = (SessionFactory) Proxy.newProxyInstance(
                SessionFactory.class.getClassLoader(),
                new Class<?>[]{SessionFactory.class},
                (proxy, method, args) -> {
                    if ("openSession".equals(method.getName())) {
                        return mockSession;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    return null;
                }
        );

        dbManager = new DatabaseConnectionManager(() -> mockSessionFactory);
        dbManager.resetMetricas();
    }

    @Test
    @DisplayName("Transação: Deve comitar com sucesso e incrementar transacoesSucesso")
    void deveIncrementarTransacoesSucessoQuandoTransacaoForConcluida() {
        assertEquals(0, dbManager.getTransacoesSucesso());
        assertEquals(0, dbManager.getTransacoesFalha());

        String resultado = dbManager.executeTransactionalResult(session -> "OK");

        assertEquals("OK", resultado);
        assertTrue(commitChamado.get(), "O commit deve ser acionado");
        assertFalse(rollbackChamado.get(), "O rollback não deve ser chamado em caso de sucesso");
        assertTrue(sessionFechada.get(), "A sessão deve ser fechada no try-with-resources");
        assertEquals(1, dbManager.getTransacoesSucesso(), "Contador de sucesso deve ser 1");
        assertEquals(0, dbManager.getTransacoesFalha(), "Contador de falha deve permanecer 0");
    }

    @Test
    @DisplayName("Transação: Deve efetuar rollback e incrementar transacoesFalha em caso de exceção")
    void deveEfetuarRollbackEIncrementarTransacoesFalhaEmCasoDeExcecao() {
        assertEquals(0, dbManager.getTransacoesSucesso());
        assertEquals(0, dbManager.getTransacoesFalha());

        assertThrows(RuntimeException.class, () -> {
            dbManager.executeTransactional(session -> {
                throw new RuntimeException("Falha na regra interna");
            });
        });

        assertFalse(commitChamado.get(), "O commit não pode ser chamado em caso de erro");
        assertTrue(rollbackChamado.get(), "O rollback deve ser acionado em caso de erro");
        assertTrue(sessionFechada.get(), "A sessão deve ser devidamente fechada");
        assertEquals(0, dbManager.getTransacoesSucesso(), "Contador de sucesso deve ser 0");
        assertEquals(1, dbManager.getTransacoesFalha(), "Contador de falha deve ser 1");
    }

    @Test
    @DisplayName("Lote e Cache: Falha em lote deve reverter transação sem invalidar cache prévio")
    void deveReverterLoteSemInvalidarCachePreexistente() {
        CacheManager cache = CacheManager.getInstance();
        cache.clear();
        cache.put("stats:geral:50", "DADO_CACHE_VALIDO", Duration.ofMinutes(5));

        lancarErroNaOperacao = true;
        SalvarMedicaoDaoImpl dao = new SalvarMedicaoDaoImpl(dbManager);

        MedicaoAmbiental m1 = new MedicaoAmbiental();
        m1.setCidade("Curitiba");

        assertThrows(RuntimeException.class, () -> dao.salvarLote(List.of(m1)));

        assertTrue(rollbackChamado.get(), "Transação em lote deve sofrer rollback atômico");
        assertEquals(1, dbManager.getTransacoesFalha());

        // Cache permanece intocado porque a falha abortou antes da invalidação
        assertTrue(cache.get("stats:geral:50").isPresent(), "Cache não deve ser invalidado quando a persistência falha");
    }

    @Test
    @DisplayName("Health Check: Deve expor métricas reais de transações de banco")
    void deveRefletirMetricasNoHealthEndpoint() throws IOException {
        dbManager.executeTransactional(session -> {});
        try {
            dbManager.executeTransactional(session -> { throw new RuntimeException("Erro proposital"); });
        } catch (Exception ignored) {}

        MockHttpExchange mockExchange = new MockHttpExchange();

        HealthHandler handler = new HealthHandler(dbManager);
        handler.handle(mockExchange);

        String json = mockExchange.responseBody.toString();
        assertTrue(json.contains("\"metricasBanco\""), "JSON do /health deve conter seção metricasBanco");
        assertTrue(json.contains("\"transacoesSucesso\":1"), "JSON do /health deve conter 1 transação de sucesso");
        assertTrue(json.contains("\"transacoesFalha\":1"), "JSON do /health deve conter 1 transação de falha");
    }

    private static class MockHttpExchange extends HttpExchange {
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private int responseCode = -1;

        @Override public Headers getRequestHeaders() { return requestHeaders; }
        @Override public Headers getResponseHeaders() { return responseHeaders; }
        @Override public URI getRequestURI() { return URI.create("/health"); }
        @Override public String getRequestMethod() { return "GET"; }
        @Override public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
        @Override public void close() {}
        @Override public java.io.InputStream getRequestBody() { return java.io.InputStream.nullInputStream(); }
        @Override public OutputStream getResponseBody() { return responseBody; }
        @Override public void sendResponseHeaders(int rCode, long responseLength) { this.responseCode = rCode; }
        @Override public java.net.InetSocketAddress getRemoteAddress() { return new java.net.InetSocketAddress("127.0.0.1", 1234); }
        @Override public int getResponseCode() { return responseCode; }
        @Override public java.net.InetSocketAddress getLocalAddress() { return new java.net.InetSocketAddress("127.0.0.1", 8080); }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public void setAttribute(String name, Object value) {}
        @Override public void setStreams(java.io.InputStream i, OutputStream o) {}
        @Override public com.sun.net.httpserver.HttpPrincipal getPrincipal() { return null; }
    }
}
