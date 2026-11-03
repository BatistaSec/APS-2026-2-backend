package com.app.dao;

import com.app.model.Cidade;
import com.app.model.Estacao;
import com.app.util.HibernateUtil;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.logging.AsyncLogger;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

public class EstacaoRepository {

    private static final EstacaoRepository INSTANCE = new EstacaoRepository();
    private final DatabaseConnectionManager dbManager;

    private EstacaoRepository() {
        this(DatabaseConnectionManager.getInstance());
    }

    public EstacaoRepository(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    public static EstacaoRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Resolve Cidade + Estacao numa única transação (upsert seguro).
     *
     * Race condition tratada: se dois fluxos tentarem criar a mesma cidade/estação
     * simultaneamente, o que perder no UNIQUE KEY faz retry de SELECT e retorna
     * o registro já criado pelo outro.
     */
    public Estacao resolverEstacao(String estacaoId, String nomeCidade,
                                   String estado, String pais, Estacao.Tipo tipo) {
        if (estacaoId == null || estacaoId.isBlank()) return null;

        String idLimpo     = estacaoId.trim();
        String nomeLimpo   = (nomeCidade != null && !nomeCidade.isBlank()) ? nomeCidade.trim() : null;
        String estadoLimpo = (estado != null && !estado.isBlank())         ? estado.trim()     : null;
        String paisLimpo   = (pais   != null && !pais.isBlank())           ? pais.trim().toUpperCase() : "BR";

        // Tenta criar numa transação — em caso de duplicate key (race condition), faz retry de leitura
        try {
            return dbManager.executeTransactionalResult(session -> {

                // 1. Busca estação já existente
                Estacao estacao = session.find(Estacao.class, idLimpo);
                if (estacao != null) return estacao;

                // 2. Resolve a cidade dentro da mesma sessão
                Cidade cidade = resolverCidadeNaSessao(session, nomeLimpo, estadoLimpo, paisLimpo);

                // 3. Cria estação
                Estacao nova = new Estacao(idLimpo, cidade, tipo);
                session.persist(nova);
                session.flush();
                AsyncLogger.info("[EstacaoRepository] Estação criada: " + nova + " | Cidade: " + cidade);
                return nova;
            });

        } catch (RuntimeException e) {
            // Se foi duplicate key (race condition na criação da cidade ou estação),
            // tenta buscar o registro que foi criado pela outra thread
            if (isCausaConstraintViolation(e)) {
                AsyncLogger.info("[EstacaoRepository] Duplicate key detectado — buscando registro existente para: " + idLimpo);
                return buscarEstacaoExistente(idLimpo);
            }
            throw e;
        }
    }

    // ── helpers privados ───────────────────────────────────────────────────

    private Cidade resolverCidadeNaSessao(Session session, String nome, String estado, String pais) {
        if (nome == null) return null;

        // Busca por nome + país (ignoramos estado na busca para evitar duplicatas com estado null vs preenchido)
        Cidade existente = session.createQuery(
                "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND pais = :pais", Cidade.class)
                .setParameter("nome", nome)
                .setParameter("pais", pais)
                .setMaxResults(1)
                .uniqueResultOptional()
                .orElse(null);

        if (existente != null) {
            // Atualiza o estado se chegou agora e estava null antes
            if (existente.getEstado() == null && estado != null) {
                existente.setEstado(estado);
            }
            return existente;
        }

        Cidade nova = new Cidade(nome, estado, pais);
        session.persist(nova);
        session.flush();
        AsyncLogger.info("[EstacaoRepository] Cidade criada: " + nova);
        return nova;
    }

    private Estacao buscarEstacaoExistente(String estacaoId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.find(Estacao.class, estacaoId);
        } catch (Exception e) {
            AsyncLogger.error("[EstacaoRepository] Erro ao buscar estação após race condition: " + e.getMessage(), e);
            return null;
        }
    }

    private boolean isCausaConstraintViolation(Throwable e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException) return true;
            if (causa instanceof java.sql.SQLIntegrityConstraintViolationException) return true;
            causa = causa.getCause();
        }
        return false;
    }
}
