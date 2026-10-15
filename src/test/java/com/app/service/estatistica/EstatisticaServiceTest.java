package com.app.service.estatistica;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EstatisticaServiceTest {

    @Test
    @DisplayName("Estatística: Deve calcular médias, picos e contagem de chuva com precisão")
    void deveCalcularEstatisticasComPrecisao() {
        ConsultaMedicaoDao daoMock = new ConsultaMedicaoDao() {
            @Override public Optional<MedicaoAmbiental> buscarPorId(Long id) { return Optional.empty(); }
            @Override public Optional<MedicaoAmbiental> buscarUltima() { return Optional.empty(); }
            @Override public Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade) { return Optional.empty(); }
            @Override
            public List<MedicaoAmbiental> listarUltimas(int limite) {
                MedicaoAmbiental m1 = new MedicaoAmbiental("EST-01", "Curitiba", LocalDateTime.now(), 15.0, 80.0, true, 20.0, 1015.0, "S", 2.0, 15.0);
                MedicaoAmbiental m2 = new MedicaoAmbiental("EST-02", "Curitiba", LocalDateTime.now(), 25.0, 60.0, false, 10.0, 1012.0, "N", 6.0, 26.0);
                return List.of(m1, m2);
            }
            @Override public List<MedicaoAmbiental> listarPorCidade(String cidade, int limite) { return listarUltimas(limite); }
            @Override public List<String> listarCidades() { return List.of("Curitiba"); }
            @Override public List<MedicaoAmbiental> listarTodas() { return Collections.emptyList(); }
            @Override public List<MedicaoAmbiental> buscarPorPeriodo(LocalDateTime i, LocalDateTime f) { return Collections.emptyList(); }
            @Override public long contarTotal() { return 2L; }
        };

        EstatisticaService service = new EstatisticaServiceImpl(daoMock);
        EstatisticasClimaticas stats = service.calcularEstatisticasPorCidade("Curitiba", 10);

        assertEquals(2, stats.totalAnalisadas());
        assertEquals(15.0, stats.tempMin(), 0.01);
        assertEquals(25.0, stats.tempMax(), 0.01);
        assertEquals(20.0, stats.tempMedia(), 0.01);
        assertEquals(60.0, stats.umidadeMin(), 0.01);
        assertEquals(80.0, stats.umidadeMax(), 0.01);
        assertEquals(70.0, stats.umidadeMedia(), 0.01);
        assertEquals(20.0, stats.ventoMaximo(), 0.01);
        assertEquals(1, stats.ocorrenciasChuva());
    }

    @Test
    @DisplayName("Estatística: Deve retornar zeros quando não houver medições")
    void deveRetornarZerosQuandoVazio() {
        ConsultaMedicaoDao daoMock = new ConsultaMedicaoDao() {
            @Override public Optional<MedicaoAmbiental> buscarPorId(Long id) { return Optional.empty(); }
            @Override public Optional<MedicaoAmbiental> buscarUltima() { return Optional.empty(); }
            @Override public Optional<MedicaoAmbiental> buscarUltimaPorCidade(String cidade) { return Optional.empty(); }
            @Override public List<MedicaoAmbiental> listarUltimas(int limite) { return Collections.emptyList(); }
            @Override public List<MedicaoAmbiental> listarPorCidade(String cidade, int limite) { return Collections.emptyList(); }
            @Override public List<String> listarCidades() { return Collections.emptyList(); }
            @Override public List<MedicaoAmbiental> listarTodas() { return Collections.emptyList(); }
            @Override public List<MedicaoAmbiental> buscarPorPeriodo(LocalDateTime i, LocalDateTime f) { return Collections.emptyList(); }
            @Override public long contarTotal() { return 0L; }
        };

        EstatisticaService service = new EstatisticaServiceImpl(daoMock);
        EstatisticasClimaticas stats = service.calcularEstatisticasRecentes(10);

        assertEquals(0, stats.totalAnalisadas());
    }
}
