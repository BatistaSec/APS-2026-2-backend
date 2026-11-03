package com.app.dao;

import com.app.model.Cidade;
import com.app.model.Estacao;
import com.app.util.HibernateUtil;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.logging.AsyncLogger;
import org.hibernate.Session;

import java.util.Optional;

/**
 * Repositório para Cidade e Estacao.
 * Busca ou cria (upsert) registros antes de persistir uma MedicaoAmbiental.
 */
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
     * Busca a Cidade pelo nome (e opcionalmente estado/país).
     * Se não existir, cria uma nova dentro de uma transação.
     */
    public Cidade buscarOuCriarCidade(String nome, String estado, String pais) {
        if (nome == null || nome.isBlank()) return null;

        String nomeLimpo   = nome.trim();
        String estadoLimpo = (estado != null && !estado.isBlank()) ? estado.trim().toUpperCase() : null;
        String paisLimpo   = (pais   != null && !pais.isBlank())   ? pais.trim().toUpperCase()   : "BR";

        // Tenta buscar existente
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Monta HQL evitando comparação nula problemática no MySQL
            String hql;
            if (estadoLimpo != null) {
                hql = "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado = :estado AND pais = :pais";
            } else {
                hql = "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado IS NULL AND pais = :pais";
            }

            var q = session.createQuery(hql, Cidade.class)
                           .setParameter("nome", nomeLimpo)
                           .setParameter("pais", paisLimpo);
            if (estadoLimpo != null) q.setParameter("estado", estadoLimpo);

            Optional<Cidade> existente = q.uniqueResultOptional();
            if (existente.isPresent()) return existente.get();

        } catch (Exception e) {
            AsyncLogger.error("[EstacaoRepository] Erro ao buscar cidade '" + nomeLimpo + "': " + e.getMessage(), e);
        }

        // Cria nova cidade
        Cidade nova = new Cidade(nomeLimpo, estadoLimpo, paisLimpo);
        try {
            dbManager.executeTransactional(s -> s.persist(nova));
            AsyncLogger.info("[EstacaoRepository] Cidade criada: " + nova);
            return nova;
        } catch (Exception e) {
            AsyncLogger.error("[EstacaoRepository] Erro ao criar cidade '" + nomeLimpo + "': " + e.getMessage(), e);
            // Tenta buscar de novo (pode ter sido criada por outra thread)
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                String hql = estadoLimpo != null
                        ? "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado = :estado AND pais = :pais"
                        : "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado IS NULL AND pais = :pais";
                var q = session.createQuery(hql, Cidade.class)
                               .setParameter("nome", nomeLimpo)
                               .setParameter("pais", paisLimpo);
                if (estadoLimpo != null) q.setParameter("estado", estadoLimpo);
                return q.uniqueResultOptional().orElse(null);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Busca a Estacao pelo ID ou cria uma nova.
     */
    public Estacao buscarOuCriarEstacao(String estacaoId, Cidade cidade, Estacao.Tipo tipo) {
        if (estacaoId == null || estacaoId.isBlank()) return null;
        String idLimpo = estacaoId.trim();

        // Tenta buscar existente
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Estacao existente = session.find(Estacao.class, idLimpo);
            if (existente != null) return existente;
        } catch (Exception e) {
            AsyncLogger.error("[EstacaoRepository] Erro ao buscar estação '" + idLimpo + "': " + e.getMessage(), e);
        }

        // Cria nova estação
        Estacao nova = new Estacao(idLimpo, cidade, tipo);
        try {
            dbManager.executeTransactional(s -> s.persist(nova));
            AsyncLogger.info("[EstacaoRepository] Estação criada: " + nova);
            return nova;
        } catch (Exception e) {
            AsyncLogger.error("[EstacaoRepository] Erro ao criar estação '" + idLimpo + "': " + e.getMessage(), e);
            // Tenta buscar de novo
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                return session.find(Estacao.class, idLimpo);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
