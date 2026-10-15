package com.app.service.estatistica;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.dao.consulta.ConsultaMedicaoDaoImpl;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.response.EstatisticasClimaticas;

import java.util.DoubleSummaryStatistics;
import java.util.List;


public class EstatisticaServiceImpl implements EstatisticaService {

    private final ConsultaMedicaoDao consultaMedicaoDao;

    public EstatisticaServiceImpl() {
        this(new ConsultaMedicaoDaoImpl());
    }

    public EstatisticaServiceImpl(ConsultaMedicaoDao consultaMedicaoDao) {
        this.consultaMedicaoDao = consultaMedicaoDao;
    }

    @Override
    public EstatisticasClimaticas calcularEstatisticasRecentes(int limite) {
        List<MedicaoAmbiental> medicoes = consultaMedicaoDao.listarUltimas(limite > 0 ? limite : 50);
        return calcularEstatisticas(medicoes);
    }

    @Override
    public EstatisticasClimaticas calcularEstatisticasPorCidade(String cidade, int limite) {
        if (cidade == null || cidade.isBlank()) {
            return calcularEstatisticasRecentes(limite);
        }
        List<MedicaoAmbiental> medicoes = consultaMedicaoDao.listarPorCidade(cidade, limite > 0 ? limite : 50);
        return calcularEstatisticas(medicoes);
    }

    private EstatisticasClimaticas calcularEstatisticas(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) {
            return new EstatisticasClimaticas(
                    0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0,
                    0.0, 0L
            );
        }

        DoubleSummaryStatistics statsTemp = medicoes.stream()
                .filter(m -> m.getTemperatura() != null)
                .mapToDouble(MedicaoAmbiental::getTemperatura)
                .summaryStatistics();

        DoubleSummaryStatistics statsUmi = medicoes.stream()
                .filter(m -> m.getUmidade() != null)
                .mapToDouble(MedicaoAmbiental::getUmidade)
                .summaryStatistics();

        double ventoMax = medicoes.stream()
                .filter(m -> m.getVelocidadeVento() != null)
                .mapToDouble(MedicaoAmbiental::getVelocidadeVento)
                .max()
                .orElse(0.0);

        long chuvaCount = medicoes.stream()
                .filter(m -> Boolean.TRUE.equals(m.getPrecipitacao()))
                .count();

        double tMin = statsTemp.getCount() > 0 ? statsTemp.getMin() : 0.0;
        double tMax = statsTemp.getCount() > 0 ? statsTemp.getMax() : 0.0;
        double tMedia = statsTemp.getCount() > 0 ? statsTemp.getAverage() : 0.0;

        double uMin = statsUmi.getCount() > 0 ? statsUmi.getMin() : 0.0;
        double uMax = statsUmi.getCount() > 0 ? statsUmi.getMax() : 0.0;
        double uMedia = statsUmi.getCount() > 0 ? statsUmi.getAverage() : 0.0;

        return new EstatisticasClimaticas(
                medicoes.size(),
                round(tMin), round(tMax), round(tMedia),
                round(uMin), round(uMax), round(uMedia),
                round(ventoMax), chuvaCount
        );
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
