package com.app.dao.salvar;

import com.app.model.MedicaoAmbiental;
import com.app.util.cache.CacheManager;
import com.app.util.database.DatabaseConnectionManager;

import java.util.List;

public class SalvarMedicaoDaoImpl implements SalvarMedicaoDao {

    private final DatabaseConnectionManager connectionManager;

    public SalvarMedicaoDaoImpl() {
        this(DatabaseConnectionManager.getInstance());
    }

    public SalvarMedicaoDaoImpl(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public MedicaoAmbiental salvar(MedicaoAmbiental medicao) {
        if (medicao == null) return null;

        connectionManager.executeTransactional(session -> {
            session.persist(medicao);
        });

        // Invalidação do cache ocorre estritamente após o commit bem-sucedido
        CacheManager.getInstance().invalidatePrefix("cidades");
        CacheManager.getInstance().invalidatePrefix("stats");

        return medicao;
    }

    @Override
    public List<MedicaoAmbiental> salvarLote(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) return List.of();

        connectionManager.executeTransactional(session -> {
            for (int i = 0; i < medicoes.size(); i++) {
                session.persist(medicoes.get(i));
                if (i > 0 && i % 25 == 0) {
                    session.flush();
                    session.clear();
                }
            }
        });

        // Invalidação do cache ocorre estritamente após o commit atômico do lote
        CacheManager.getInstance().invalidatePrefix("cidades");
        CacheManager.getInstance().invalidatePrefix("stats");

        return medicoes;
    }
}
