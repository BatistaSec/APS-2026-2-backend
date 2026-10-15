package com.app.dao.salvar;

import com.app.model.MedicaoAmbiental;
import com.app.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;


public class SalvarMedicaoDaoImpl implements SalvarMedicaoDao {

    @Override
    public MedicaoAmbiental salvar(MedicaoAmbiental medicao) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(medicao);
            tx.commit();
            return medicao;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Falha ao salvar medição ambiental: " + e.getMessage(), e);
        }
    }
}
