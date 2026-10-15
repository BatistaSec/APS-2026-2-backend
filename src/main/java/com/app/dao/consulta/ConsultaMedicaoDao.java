package com.app.dao.consulta;

import com.app.model.MedicaoAmbiental;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface ConsultaMedicaoDao {

    Optional<MedicaoAmbiental> buscarPorId(Long id);

    Optional<MedicaoAmbiental> buscarUltima();

    Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade);

    List<MedicaoAmbiental> listarUltimas(int limite);

    List<MedicaoAmbiental> listarPorCidade(String cidade, int limite);

    List<String> listarCidades();

    List<MedicaoAmbiental> listarTodas();

    List<MedicaoAmbiental> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);

    long contarTotal();
}
