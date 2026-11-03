package com.app.dao.exclusao;

import com.app.model.MedicaoAmbiental;
import com.app.util.database.DatabaseConnectionManager;
import com.app.util.logging.AsyncLogger;

public class DeletarMedicaoDaoImpl implements DeletarMedicaoDao {

    private final DatabaseConnectionManager connectionManager;

    public DeletarMedicaoDaoImpl() {
        this(DatabaseConnectionManager.getInstance());
    }

    public DeletarMedicaoDaoImpl(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public boolean deletarPorId(Long id) {
        if (id == null) return false;
        try {
            return Boolean.TRUE.equals(connectionManager.executeTransactionalResult(session -> {
                MedicaoAmbiental medicao = session.find(MedicaoAmbiental.class, id);
                if (medicao != null) {
                    session.remove(medicao);
                    return true;
                }
                return false;
            }));
        } catch (Exception e) {
            AsyncLogger.error("Erro ao deletar medição #" + id + ": " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int deletarPorCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) return 0;
        try {
            return connectionManager.executeTransactionalResult(session ->
                session.createMutationQuery(
                    "DELETE FROM MedicaoAmbiental WHERE LOWER(cidade) LIKE LOWER(:cidade)")
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .executeUpdate()
            );
        } catch (Exception e) {
            AsyncLogger.error("Erro ao deletar medições da cidade " + cidade + ": " + e.getMessage(), e);
            return 0;
        }
    }
}
