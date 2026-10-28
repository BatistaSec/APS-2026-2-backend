package com.app.dao.consulta;

import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;
import com.app.util.HibernateUtil;
import com.app.util.cache.CacheManager;
import com.app.util.logging.AsyncLogger;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConsultaMedicaoDaoImpl implements ConsultaMedicaoDao {

    private static final Set<String> CAMPOS_ORDENACAO_VALIDOS = Set.of(
            "datahora", "temperatura", "umidade", "sensacaotermica", "cidade", "id", "velocidadevento", "pressaoatmosferica"
    );

    @Override
    public Optional<MedicaoAmbiental> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.find(MedicaoAmbiental.class, id));
        } catch (Exception e) {
            AsyncLogger.error("Erro ao buscar medição por ID #" + id + ": " + e.getMessage(), e);
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
            AsyncLogger.error("Erro ao buscar última medição: " + e.getMessage(), e);
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
            AsyncLogger.error("Erro ao buscar última medição da cidade " + cidade + ": " + e.getMessage(), e);
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
            AsyncLogger.error("Erro ao listar últimas medições: " + e.getMessage(), e);
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
            AsyncLogger.error("Erro ao listar medições da cidade " + cidade + ": " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarPorEstacao(String estacaoId, int limite) {
        if (estacaoId == null || estacaoId.isBlank()) {
            return Collections.emptyList();
        }
        int max = limite > 0 ? Math.min(limite, 500) : 50;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM MedicaoAmbiental WHERE LOWER(estacaoId) = LOWER(:estacaoId) ORDER BY dataHora DESC, id DESC",
                    MedicaoAmbiental.class)
                    .setParameter("estacaoId", estacaoId.trim())
                    .setMaxResults(max)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar medições por estação " + estacaoId + ": " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MedicaoAmbiental> listarComFiltros(String cidade, Double tempMin, Double tempMax, Boolean precipitacao,
                                                   String sort, String order, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int firstResult = (safePage - 1) * safePageSize;

        String safeSort = validarCampoOrdenacao(sort);
        String safeOrder = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";

        StringBuilder hql = new StringBuilder("FROM MedicaoAmbiental WHERE 1=1 ");
        if (cidade != null && !cidade.isBlank()) {
            hql.append("AND LOWER(cidade) LIKE LOWER(:cidade) ");
        }
        if (tempMin != null) {
            hql.append("AND temperatura >= :tempMin ");
        }
        if (tempMax != null) {
            hql.append("AND temperatura <= :tempMax ");
        }
        if (precipitacao != null) {
            hql.append("AND precipitacao = :precipitacao ");
        }

        hql.append("ORDER BY ").append(safeSort).append(" ").append(safeOrder).append(", id DESC");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<MedicaoAmbiental> query = session.createQuery(hql.toString(), MedicaoAmbiental.class);

            if (cidade != null && !cidade.isBlank()) {
                query.setParameter("cidade", "%" + cidade.trim() + "%");
            }
            if (tempMin != null) {
                query.setParameter("tempMin", tempMin);
            }
            if (tempMax != null) {
                query.setParameter("tempMax", tempMax);
            }
            if (precipitacao != null) {
                query.setParameter("precipitacao", precipitacao);
            }

            return query.setFirstResult(firstResult)
                    .setMaxResults(safePageSize)
                    .getResultList();
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar medições com filtros: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public long contarComFiltros(String cidade, Double tempMin, Double tempMax, Boolean precipitacao) {
        StringBuilder hql = new StringBuilder("SELECT COUNT(m) FROM MedicaoAmbiental m WHERE 1=1 ");
        if (cidade != null && !cidade.isBlank()) {
            hql.append("AND LOWER(m.cidade) LIKE LOWER(:cidade) ");
        }
        if (tempMin != null) {
            hql.append("AND m.temperatura >= :tempMin ");
        }
        if (tempMax != null) {
            hql.append("AND m.temperatura <= :tempMax ");
        }
        if (precipitacao != null) {
            hql.append("AND m.precipitacao = :precipitacao ");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(hql.toString(), Long.class);

            if (cidade != null && !cidade.isBlank()) {
                query.setParameter("cidade", "%" + cidade.trim() + "%");
            }
            if (tempMin != null) {
                query.setParameter("tempMin", tempMin);
            }
            if (tempMax != null) {
                query.setParameter("tempMax", tempMax);
            }
            if (precipitacao != null) {
                query.setParameter("precipitacao", precipitacao);
            }

            Long total = query.uniqueResult();
            return total != null ? total : 0L;
        } catch (Exception e) {
            AsyncLogger.error("Erro ao contar medições filtradas: " + e.getMessage(), e);
            return 0L;
        }
    }

    @Override
    public EstatisticasClimaticas calcularEstatisticasAgregadas(String cidade, int limite) {
        int safeLimite = limite > 0 ? Math.min(limite, 500) : 50;
        String cacheKey = "stats:" + (cidade != null ? cidade.trim().toLowerCase() : "geral") + ":" + safeLimite;
        Optional<EstatisticasClimaticas> emCache = CacheManager.getInstance().get(cacheKey);
        if (emCache.isPresent()) {
            return emCache.get();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 1. Seleciona deterministicamente os IDs das N medições mais recentes
            StringBuilder idHql = new StringBuilder("SELECT m.id FROM MedicaoAmbiental m ");
            if (cidade != null && !cidade.isBlank()) {
                idHql.append("WHERE LOWER(m.cidade) LIKE LOWER(:cidade) ");
            }
            idHql.append("ORDER BY m.dataHora DESC, m.id DESC");

            Query<Long> idQuery = session.createQuery(idHql.toString(), Long.class);
            if (cidade != null && !cidade.isBlank()) {
                idQuery.setParameter("cidade", "%" + cidade.trim() + "%");
            }
            idQuery.setMaxResults(safeLimite);
            List<Long> ids = idQuery.getResultList();

            if (ids == null || ids.isEmpty()) {
                EstatisticasClimaticas vazia = new EstatisticasClimaticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
                CacheManager.getInstance().put(cacheKey, vazia, Duration.ofSeconds(30));
                return vazia;
            }

            // 2. Agrega os cálculos diretamente sobre os N registros selecionados
            String aggHql = "SELECT count(m), " +
                    "coalesce(min(m.temperatura), 0.0), coalesce(max(m.temperatura), 0.0), coalesce(avg(m.temperatura), 0.0), " +
                    "coalesce(min(m.umidade), 0.0), coalesce(max(m.umidade), 0.0), coalesce(avg(m.umidade), 0.0), " +
                    "coalesce(max(m.velocidadeVento), 0.0), " +
                    "sum(case when m.precipitacao = true then 1L else 0L end) " +
                    "FROM MedicaoAmbiental m WHERE m.id IN (:ids)";

            Query<Object[]> query = session.createQuery(aggHql, Object[].class);
            query.setParameterList("ids", ids);

            Object[] res = query.uniqueResult();
            if (res == null || ((Number) res[0]).longValue() == 0) {
                EstatisticasClimaticas vazia = new EstatisticasClimaticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L);
                CacheManager.getInstance().put(cacheKey, vazia, Duration.ofSeconds(30));
                return vazia;
            }

            int total = ((Number) res[0]).intValue();
            double tMin = round(((Number) res[1]).doubleValue());
            double tMax = round(((Number) res[2]).doubleValue());
            double tAvg = round(((Number) res[3]).doubleValue());
            double uMin = round(((Number) res[4]).doubleValue());
            double uMax = round(((Number) res[5]).doubleValue());
            double uAvg = round(((Number) res[6]).doubleValue());
            double vMax = round(((Number) res[7]).doubleValue());
            long chuva = res[8] != null ? ((Number) res[8]).longValue() : 0L;

            EstatisticasClimaticas stats = new EstatisticasClimaticas(total, tMin, tMax, tAvg, uMin, uMax, uAvg, vMax, chuva);
            CacheManager.getInstance().put(cacheKey, stats, Duration.ofSeconds(30));
            return stats;
        } catch (Exception e) {
            AsyncLogger.error("Erro na agregação única de estatísticas no banco: " + e.getMessage(), e);
            return null; // Fallback para cálculo em memória se houver falha
        }
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private String validarCampoOrdenacao(String sort) {
        if (sort == null || sort.isBlank()) {
            return "dataHora";
        }
        String normalizado = sort.trim().toLowerCase();
        if (CAMPOS_ORDENACAO_VALIDOS.contains(normalizado)) {
            return switch (normalizado) {
                case "temperatura" -> "temperatura";
                case "umidade" -> "umidade";
                case "sensacaotermica" -> "sensacaoTermica";
                case "cidade" -> "cidade";
                case "velocidadevento" -> "velocidadeVento";
                case "pressaoatmosferica" -> "pressaoAtmosferica";
                case "id" -> "id";
                default -> "dataHora";
            };
        }
        return "dataHora";
    }

    @Override
    public List<String> listarCidades() {
        Optional<List<String>> emCache = CacheManager.getInstance().get("cidades:lista");
        if (emCache.isPresent()) {
            return emCache.get();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<String> cidades = session.createQuery(
                    "SELECT DISTINCT m.cidade FROM MedicaoAmbiental m WHERE m.cidade IS NOT NULL AND m.cidade <> '' ORDER BY m.cidade ASC",
                    String.class)
                    .getResultList();

            CacheManager.getInstance().put("cidades:lista", cidades, Duration.ofMinutes(2));
            return cidades;
        } catch (Exception e) {
            AsyncLogger.error("Erro ao listar cidades: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
