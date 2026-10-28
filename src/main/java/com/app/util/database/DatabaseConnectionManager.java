package com.app.util.database;

import com.app.util.HibernateUtil;
import com.app.util.logging.AsyncLogger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class DatabaseConnectionManager {

    private static final DatabaseConnectionManager INSTANCE = new DatabaseConnectionManager();

    private final AtomicLong transacoesSucesso = new AtomicLong(0);
    private final AtomicLong transacoesFalha = new AtomicLong(0);
    private final Supplier<SessionFactory> sessionFactorySupplier;

    private DatabaseConnectionManager() {
        this(HibernateUtil::getSessionFactory);
    }

    public DatabaseConnectionManager(Supplier<SessionFactory> sessionFactorySupplier) {
        this.sessionFactorySupplier = sessionFactorySupplier;
    }

    public static DatabaseConnectionManager getInstance() {
        return INSTANCE;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactorySupplier.get();
    }

    public Session openSession() {
        return getSessionFactory().openSession();
    }

    public void executeTransactional(Consumer<Session> action) {
        executeTransactionalResult(session -> {
            action.accept(session);
            return null;
        });
    }

    public <T> T executeTransactionalResult(Function<Session, T> action) {
        Transaction tx = null;
        try (Session session = openSession()) {
            tx = session.beginTransaction();
            T result = action.apply(session);
            tx.commit();
            transacoesSucesso.incrementAndGet();
            return result;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rbEx) {
                    AsyncLogger.error("Falha ao efetuar rollback da transação: " + rbEx.getMessage(), rbEx);
                }
            }
            transacoesFalha.incrementAndGet();
            AsyncLogger.error("Erro em transação de banco de dados: " + e.getMessage(), e);
            throw new RuntimeException("Falha na execução transacional do banco", e);
        }
    }

    public boolean isHealthy() {
        try {
            SessionFactory sf = getSessionFactory();
            if (sf == null || sf.isClosed()) return false;
            try (Session session = sf.openSession()) {
                return session.isOpen();
            }
        } catch (Throwable e) {
            AsyncLogger.error("Health check do banco de dados falhou: " + e.getMessage());
            return false;
        }
    }

    public long getTransacoesSucesso() {
        return transacoesSucesso.get();
    }

    public long getTransacoesFalha() {
        return transacoesFalha.get();
    }

    public void resetMetricas() {
        transacoesSucesso.set(0);
        transacoesFalha.set(0);
    }

    public void shutdown() {
        HibernateUtil.shutdown();
    }
}
