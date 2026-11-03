package com.app.dao;

import com.app.model.Cidade;
import com.app.model.Estacao;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.HibernateUtil;
import org.hibernate.Session;

import java.util.Optional;

/**
 * Repositório para Cidade e Estacao.
 *
 * Responsável por:
 *  - Buscar ou criar uma Cidade (por nome + estado + pais)
 *  - Buscar ou criar uma Estacao (por ID + cidade + tipo)
 *
 * Usado por RegistroMedicaoServiceImpl antes de persistir uma MedicaoAmbiental.
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
     * Retorna a Cidade existente ou cria uma nova (upsert por nome+estado+pais).
     */
    public Cidade buscarOuCriarCidade(String nome, String estado, String pais) {
        if (nome == null || nome.isBlank()) return null;
        String nomeLimpo   = nome.trim();
        String estadoLimpo = estado != null ? estado.trim().toUpperCase() : null;
        String paisLimpo   = pais   != null ? pais.trim().toUpperCase()   : "BR";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // busca existente
            String hql = "FROM Cidade WHERE LOWER(nome) = LOWER(:nome)" +
                         " AND (estado = :estado OR (:estado IS NULL AND estado IS NULL))" +
                         " AND pais = :pais";
            Optional<Cidade> existente = session.createQuery(hql, Cidade.class)
                    .setParameter("nome",   nomeLimpo)
                    .setParameter("estado", estadoLimpo)
                    .setParameter("pais",   paisLimpo)
                    .uniqueResultOptional();
            if (existente.isPresent()) return existente.get();
        } catch (Exception ignored) {}

        // cria nova
        Cidade nova = new Cidade(nomeLimpo, estadoLimpo, paisLimpo);
        dbManager.executeTransactional(s -> s.persist(nova));
        return nova;
    }

    /**
     * Retorna a Estacao existente pelo ID ou cria uma nova.
     */
    public Estacao buscarOuCriarEstacao(String estacaoId, Cidade cidade, Estacao.Tipo tipo) {
        if (estacaoId == null || estacaoId.isBlank()) return null;
        String idLimpo = estacaoId.trim();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Estacao existente = session.find(Estacao.class, idLimpo);
            if (existente != null) return existente;
        } catch (Exception ignored) {}

        Estacao nova = new Estacao(idLimpo, cidade, tipo);
        dbManager.executeTransactional(s -> s.persist(nova));
        return nova;
    }
}
