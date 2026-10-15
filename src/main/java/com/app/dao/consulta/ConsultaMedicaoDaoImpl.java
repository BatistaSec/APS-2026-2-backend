package com.app.dao.consulta;

import com.app.model.MedicaoAmbiental;
import com.app.util.HibernateUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class ConsultaMedicaoDaoImpl implements ConsultaMedicaoDao {

    @Override
    public Optional<MedicaoAmbiental> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.find(MedicaoAmbiental.class, id));
        } catch (Exception e) {
            System.err.println("Erro ao buscar medição por ID #" + id + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<MedicaoAmbiental> buscarUltima() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM MedicaoAmbiental ORDER BY dataHora DESC, id DESC", MedicaoAmbiental.class)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Erro ao buscar última medição: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            return buscarUltima();
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicaoAmbiental WHERE LOWER(cidade) LIKE LOWER(:cidade) ORDER BY dataHora DESC, id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Erro ao buscar última medição da cidade " + cidade + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarUltimas(int limite) {
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<MedicaoAmbiental> cq = cb.createQuery(MedicaoAmbiental.class);
            Root<MedicaoAmbiental> root = cq.from(MedicaoAmbiental.class);

            cq.select(root).orderBy(cb.desc(root.get("dataHora")), cb.desc(root.get("id")));

            return session.createQuery(cq)
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao listar últimas medições: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarPorCidade(String cidade, int limite) {
        if (cidade == null || cidade.isBlank()) {
            return listarUltimas(limite);
        }
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicaoAmbiental WHERE LOWER(cidade) LIKE LOWER(:cidade) ORDER BY dataHora DESC, id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao listar medições da cidade " + cidade + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> listarCidades() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT DISTINCT m.cidade FROM MedicaoAmbiental m WHERE m.cidade IS NOT NULL AND m.cidade <> '' ORDER BY m.cidade ASC",
                    String.class)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao listar cidades: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarTodas() {
        return listarUltimas(200);
    }

    @Override
    public List<MedicaoAmbiental> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicaoAmbiental WHERE dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC",
                    MedicaoAmbiental.class)
                    .setParameter("inicio", inicio)
                    .setParameter("fim", fim)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("Erro ao buscar medições por período: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long contarTotal() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long total = session.createQuery("SELECT COUNT(m) FROM MedicaoAmbiental m", Long.class).uniqueResult();
            return total != null ? total : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
