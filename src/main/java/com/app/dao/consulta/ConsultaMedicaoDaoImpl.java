package com.app.dao.consulta;

import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;
import com.app.util.HibernateUtil;
import com.app.util.logging.AsyncLogger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Consultas usando JOIN com as tabelas cidades e estacoes.
 *
 * Padrão de navegação HQL:
 *   m.estacao.cidade.nome  →  medicoes_ambientais JOIN estacoes JOIN cidades
 */
public class ConsultaMedicaoDaoImpl implements ConsultaMedicaoDao {

    private static final Set<String> CAMPOS_ORDENACAO_VALIDOS = Set.of(
            "datahora", "temperatura", "umidade", "sensacaotermica",
            "cidade", "id", "velocidadevento", "pressaoatmosferica"
    );

    @Override
    public Optional<MedicaoAmbiental> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            // JOIN FETCH para evitar lazy-loading fora da sessão
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade WHERE m.id = :id",
                    MedicaoAmbiental.class)
                    .setParameter("id", id)
                    .uniqueResultOptional();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao buscar medição #" + id + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MedicaoAmbiental> buscarUltima() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade " +
                    "ORDER BY m.dataHora DESC, m.id DESC",
                    MedicaoAmbiental.class)
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao buscar última medição: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) return buscarUltima();
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e JOIN FETCH e.cidade c " +
                    "WHERE LOWER(c.nome) LIKE LOWER(:cidade) " +
                    "ORDER BY m.dataHora DESC, m.id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .setMaxResults(1)
                    .uniqueResultOptional();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao buscar última medição de " + cidade + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarUltimas(int limite) {
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade " +
                    "ORDER BY m.dataHora DESC, m.id DESC",
                    MedicaoAmbiental.class)
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar últimas: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarPorCidade(String cidade, int limite) {
        if (cidade == null || cidade.isBlank()) return listarUltimas(limite);
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e JOIN FETCH e.cidade c " +
                    "WHERE LOWER(c.nome) LIKE LOWER(:cidade) " +
                    "ORDER BY m.dataHora DESC, m.id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("cidade", "%" + cidade.trim() + "%")
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar por cidade " + cidade + ": " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarPorEstacao(String estacaoId, int limite) {
        if (estacaoId == null || estacaoId.isBlank()) return Collections.emptyList();
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            return s.createQuery(
                    "FROM MedicaoAmbiental m JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade " +
                    "WHERE LOWER(e.id) = LOWER(:estacaoId) " +
                    "ORDER BY m.dataHora DESC, m.id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("estacaoId", estacaoId.trim())
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar por estação " + estacaoId + ": " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarComFiltros(String cidade, Double tempMin, Double tempMax,
                                                   Boolean precipitacao, String sort, String order,
                                                   int page, int pageSize) {
        int safePage     = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int firstResult  = (safePage - 1) * safePageSize;
        String safeSort  = validarCampoOrdenacao(sort);
        String safeOrder = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";

        boolean filtraCidade = cidade != null && !cidade.isBlank();

        StringBuilder hql = new StringBuilder(
                "FROM MedicaoAmbiental m JOIN FETCH m.estacao e LEFT JOIN FETCH e.cidade c WHERE 1=1 ");
        if (filtraCidade)          hql.append("AND LOWER(c.nome) LIKE LOWER(:cidade) ");
        if (tempMin != null)       hql.append("AND m.temperatura >= :tempMin ");
        if (tempMax != null)       hql.append("AND m.temperatura <= :tempMax ");
        if (precipitacao != null)  hql.append("AND m.precipitacao = :precipitacao ");

        // mapeamento campo → HQL path
        String sortHql = switch (safeSort) {
            case "cidade"            -> "c.nome";
            case "temperatura"       -> "m.temperatura";
            case "umidade"           -> "m.umidade";
            case "sensacaoTermica"   -> "m.sensacaoTermica";
            case "velocidadeVento"   -> "m.velocidadeVento";
            case "pressaoAtmosferica"-> "m.pressaoAtmosferica";
            case "id"                -> "m.id";
            default                  -> "m.dataHora";
        };
        hql.append("ORDER BY ").append(sortHql).append(" ").append(safeOrder).append(", m.id DESC");

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<MedicaoAmbiental> q = s.createQuery(hql.toString(), MedicaoAmbiental.class);
            if (filtraCidade)         q.setParameter("cidade",       "%" + cidade.trim() + "%");
            if (tempMin != null)      q.setParameter("tempMin",       tempMin);
            if (tempMax != null)      q.setParameter("tempMax",       tempMax);
            if (precipitacao != null) q.setParameter("precipitacao",  precipitacao);
            return q.setFirstResult(firstResult).setMaxResults(safePageSize).getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar com filtros: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long contarComFiltros(String cidade, Double tempMin, Double tempMax, Boolean precipitacao) {
        boolean filtraCidade = cidade != null && !cidade.isBlank();

        StringBuilder hql = new StringBuilder(
                "SELECT COUNT(m) FROM MedicaoAmbiental m JOIN m.estacao e LEFT JOIN e.cidade c WHERE 1=1 ");
        if (filtraCidade)         hql.append("AND LOWER(c.nome) LIKE LOWER(:cidade) ");
        if (tempMin != null)      hql.append("AND m.temperatura >= :tempMin ");
        if (tempMax != null)      hql.append("AND m.temperatura <= :tempMax ");
        if (precipitacao != null) hql.append("AND m.precipitacao = :precipitacao ");

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> q = s.createQuery(hql.toString(), Long.class);
            if (filtraCidade)         q.setParameter("cidade",      "%" + cidade.trim() + "%");
            if (tempMin != null)      q.setParameter("tempMin",      tempMin);
            if (tempMax != null)      q.setParameter("tempMax",      tempMax);
            if (precipitacao != null) q.setParameter("precipitacao", precipitacao);
            Long total = q.uniqueResult();
            return total != null ? total : 0L;
        } catch (Exception e) {
            AsyncLogger.error("Erro ao contar medições: " + e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public EstatisticasClimaticas calcularEstatisticasAgregadas(String cidade, int limite) {
        int safeLimite = limite > 0 ? Math.min(limite, 500) : 50;
        boolean filtraCidade = cidade != null && !cidade.isBlank();

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            // 1. IDs das N medições mais recentes
            StringBuilder idHql = new StringBuilder(
                    "SELECT m.id FROM MedicaoAmbiental m JOIN m.estacao e LEFT JOIN e.cidade c ");
            if (filtraCidade) idHql.append("WHERE LOWER(c.nome) LIKE LOWER(:cidade) ");
            idHql.append("ORDER BY m.dataHora DESC, m.id DESC");

            Query<Long> idQ = s.createQuery(idHql.toString(), Long.class);
            if (filtraCidade) idQ.setParameter("cidade", "%" + cidade.trim() + "%");
            idQ.setMaxResults(safeLimite);
            List<Long> ids = idQ.getResultList();

            if (ids == null || ids.isEmpty()) {
                return new EstatisticasClimaticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
            }

            // 2. Agregação sobre esses IDs
            String aggHql =
                    "SELECT count(m), " +
                    "coalesce(min(m.temperatura),0.0), coalesce(max(m.temperatura),0.0), coalesce(avg(m.temperatura),0.0), " +
                    "coalesce(min(m.umidade),0.0),     coalesce(max(m.umidade),0.0),     coalesce(avg(m.umidade),0.0), " +
                    "coalesce(max(m.velocidadeVento),0.0), " +
                    "sum(case when m.precipitacao = true then 1L else 0L end) " +
                    "FROM MedicaoAmbiental m WHERE m.id IN (:ids)";

            Object[] res = s.createQuery(aggHql, Object[].class)
                            .setParameterList("ids", ids)
                            .uniqueResult();

            if (res == null || ((Number) res[0]).longValue() == 0) {
                return new EstatisticasClimaticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
            }

            return new EstatisticasClimaticas(
                    ((Number) res[0]).intValue(),
                    r(((Number) res[1]).doubleValue()), r(((Number) res[2]).doubleValue()), r(((Number) res[3]).doubleValue()),
                    r(((Number) res[4]).doubleValue()), r(((Number) res[5]).doubleValue()), r(((Number) res[6]).doubleValue()),
                    r(((Number) res[7]).doubleValue()),
                    res[8] != null ? ((Number) res[8]).longValue() : 0L
            );
        } catch (Exception e) {
            AsyncLogger.error("Erro ao calcular estatísticas: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<String> listarCidades() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            // JOIN para listar apenas cidades que têm medições
            return s.createQuery(
                    "SELECT DISTINCT c.nome FROM MedicaoAmbiental m " +
                    "JOIN m.estacao e JOIN e.cidade c " +
                    "WHERE c.nome IS NOT NULL ORDER BY c.nome ASC",
                    String.class)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar cidades: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private double r(double v) { return Math.round(v * 100.0) / 100.0; }

    private String validarCampoOrdenacao(String sort) {
        if (sort == null || sort.isBlank()) return "dataHora";
        return switch (sort.trim().toLowerCase()) {
            case "temperatura"        -> "temperatura";
            case "umidade"            -> "umidade";
            case "sensacaotermica"    -> "sensacaoTermica";
            case "cidade"             -> "cidade";
            case "velocidadevento"    -> "velocidadeVento";
            case "pressaoatmosferica" -> "pressaoAtmosferica";
            case "id"                 -> "id";
            default                   -> "dataHora";
        };
    }
}
