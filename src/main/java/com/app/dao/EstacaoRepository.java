package com.app.dao;

import com.app.model.Cidade;
import com.app.model.Estacao;
import com.app.util.HibernateUtil;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.logging.AsyncLogger;
import org.hibernate.Session;

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
     * Resolve (busca ou cria) Cidade e Estacao numa única transação.
     * Retorna a Estacao pronta para ser usada na mesma sessão de salvamento
     * da MedicaoAmbiental.
     */
    public Estacao resolverEstacao(String estacaoId, String nomeCidade,
                                   String estado, String pais, Estacao.Tipo tipo) {
        if (estacaoId == null || estacaoId.isBlank()) return null;

        String idLimpo     = estacaoId.trim();
        String nomeLimpo   = (nomeCidade != null && !nomeCidade.isBlank()) ? nomeCidade.trim() : null;
        String estadoLimpo = (estado != null && !estado.isBlank())         ? estado.trim()     : null;
        String paisLimpo   = (pais   != null && !pais.isBlank())           ? pais.trim().toUpperCase() : "BR";

        return dbManager.executeTransactionalResult(session -> {

            // 1. Busca estação existente
            Estacao estacao = session.find(Estacao.class, idLimpo);
            if (estacao != null) return estacao;

            // 2. Cidade: busca ou cria dentro da mesma sessão
            Cidade cidade = null;
            if (nomeLimpo != null) {
                String hql = estadoLimpo != null
                        ? "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado = :estado AND pais = :pais"
                        : "FROM Cidade WHERE LOWER(nome) = LOWER(:nome) AND estado IS NULL AND pais = :pais";

                var q = session.createQuery(hql, Cidade.class)
                               .setParameter("nome", nomeLimpo)
                               .setParameter("pais", paisLimpo);
                if (estadoLimpo != null) q.setParameter("estado", estadoLimpo);

                cidade = q.uniqueResultOptional().orElse(null);

                if (cidade == null) {
                    cidade = new Cidade(nomeLimpo, estadoLimpo, paisLimpo);
                    session.persist(cidade);
                    session.flush(); // garante ID antes de criar Estacao
                    AsyncLogger.info("[EstacaoRepository] Cidade criada: " + cidade);
                }
            }

            // 3. Cria estação vinculada à cidade
            Estacao nova = new Estacao(idLimpo, cidade, tipo);
            session.persist(nova);
            AsyncLogger.info("[EstacaoRepository] Estação criada: " + nova);
            return nova;
        });
    }
}
