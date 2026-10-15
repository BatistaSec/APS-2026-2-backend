package com.app.dao.exclusao;

import com.app.model.MedicaoAmbiental;
import com.app.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;


public class DeletarMedicaoDaoImpl implements DeletarMedicaoDao {

    @Override
    public boolean deletarPorId(Long id) {
        if (id == null) return false;
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            MedicaoAmbiental medicao = session.find(MedicaoAmbiental.class, id);
            if (medicao != null) {
                session.remove(medicao);
                tx.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            System.err.println("Erro ao deletar medição #" + id + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public int deletarPorCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) return 0;
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            int deletados = session.createMutationQuery(
                    "DELETE FROM MedicaoAmbiental WHERE LOWER(cidade) LIKE LOWER(:cidade)")
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .executeUpdate();
            tx.commit();
            return deletados;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            System.err.println("Erro ao deletar medições da cidade " + cidade + ": " + e.getMessage());
            return 0;
        }
    }
}
