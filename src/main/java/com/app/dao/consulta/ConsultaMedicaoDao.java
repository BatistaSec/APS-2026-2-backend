package com.app.dao.consulta;

import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ConsultaMedicaoDao {

    Optional<MedicaoAmbiental> buscarPorId(Long id);

    Optional<MedicaoAmbiental> buscarUltima();

    Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade);

    List<MedicaoAmbiental> listarUltimas(int limite);

    List<MedicaoAmbiental> listarPorCidade(String cidade, int limite);

    default List<MedicaoAmbiental> listarPorEstacao(String estacaoId, int limite) {
        return Collections.emptyList();
    }

    default List<MedicaoAmbiental> listarComFiltros(String cidade, Double tempMin, Double tempMax, Boolean precipitacao,
                                                    String sort, String order, int page, int pageSize) {
        return Collections.emptyList();
    }

    default long contarComFiltros(String cidade, Double tempMin, Double tempMax, Boolean precipitacao) {
        return 0L;
    }

    default EstatisticasClimaticas calcularEstatisticasAgregadas(String cidade, int limite) {
        return null;
    }

    List<String> listarCidades();
}
