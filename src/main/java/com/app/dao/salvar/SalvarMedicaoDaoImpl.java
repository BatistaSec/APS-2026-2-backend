package com.app.dao.salvar;

import com.app.model.MedicaoAmbiental;
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
        return medicoes;
    }
}
