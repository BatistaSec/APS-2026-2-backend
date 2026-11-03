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
            // Re-attach a Estacao (e sua Cidade) para evitar "detached entity" ao persistir
            if (medicao.getEstacao() != null && medicao.getEstacao().getId() != null) {
                var estacaoManaged = session.find(
                        com.app.model.Estacao.class, medicao.getEstacao().getId());
                if (estacaoManaged != null) {
                    medicao.setEstacao(estacaoManaged);
                } else {
                    session.persist(medicao.getEstacao());
                }
            }
            session.persist(medicao);
        });
        return medicao;
    }

    @Override
    public List<MedicaoAmbiental> salvarLote(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) return List.of();
        connectionManager.executeTransactional(session -> {
            for (int i = 0; i < medicoes.size(); i++) {
                MedicaoAmbiental m = medicoes.get(i);
                // Re-attach estação para cada medição do lote
                if (m.getEstacao() != null && m.getEstacao().getId() != null) {
                    var estacaoManaged = session.find(
                            com.app.model.Estacao.class, m.getEstacao().getId());
                    if (estacaoManaged != null) {
                        m.setEstacao(estacaoManaged);
                    } else {
                        session.persist(m.getEstacao());
                    }
                }
                session.persist(m);
                if (i > 0 && i % 25 == 0) {
                    session.flush();
                    session.clear();
                }
            }
        });
        return medicoes;
    }
}
