package com.app.service.registro;

import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.model.MedicaoAmbiental;
import com.app.service.calculo.CalculadoraSensacaoTermica;
import com.app.service.calculo.CalculadoraSensacaoTermicaImpl;

import java.time.LocalDateTime;
import java.util.List;

public class RegistroMedicaoServiceImpl implements RegistroMedicaoService {

    private final SalvarMedicaoDao salvarMedicaoDao;
    private final CalculadoraSensacaoTermica calculadoraSensacaoTermica;

    public RegistroMedicaoServiceImpl() {
        this(new SalvarMedicaoDaoImpl(), new CalculadoraSensacaoTermicaImpl());
    }

    public RegistroMedicaoServiceImpl(SalvarMedicaoDao salvarMedicaoDao, CalculadoraSensacaoTermica calculadoraSensacaoTermica) {
        this.salvarMedicaoDao = salvarMedicaoDao;
        this.calculadoraSensacaoTermica = calculadoraSensacaoTermica;
    }

    @Override
    public MedicaoAmbiental registrar(MedicaoAmbiental medicao) {
        prepararMedicao(medicao);
        return salvarMedicaoDao.salvar(medicao);
    }

    @Override
    public List<MedicaoAmbiental> registrarLote(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) {
            return List.of();
        }
        for (MedicaoAmbiental medicao : medicoes) {
            prepararMedicao(medicao);
        }
        return salvarMedicaoDao.salvarLote(medicoes);
    }

    public void prepararMedicao(MedicaoAmbiental medicao) {
        if (medicao == null) {
            throw new IllegalArgumentException(com.app.util.i18n.I18n.get("validacao.medicao.nula"));
        }

        validarLimitesFisicos(medicao.getTemperatura(), medicao.getUmidade());

        if (medicao.getDataHora() == null) {
            medicao.setDataHora(LocalDateTime.now());
        }

        if (medicao.getEstacaoId() == null || medicao.getEstacaoId().isBlank()) {
            String cid = medicao.getCidade() != null ? medicao.getCidade().toUpperCase().replace(" ", "-") : "PADRAO";
            medicao.setEstacaoId("ESTACAO-" + cid);
        }

        if (medicao.getPrecipitacao() == null) {
            medicao.setPrecipitacao(false);
        }

        if (medicao.getVelocidadeVento() == null) {
            medicao.setVelocidadeVento(0.0);
        }

        if (medicao.getSensacaoTermica() == null && medicao.getTemperatura() != null) {
            double temp = medicao.getTemperatura();
            double umi = medicao.getUmidade() != null ? medicao.getUmidade() : 50.0;
            double vento = medicao.getVelocidadeVento() != null ? medicao.getVelocidadeVento() : 0.0;
            double calculada = calculadoraSensacaoTermica.calcular(temp, umi, vento);
            medicao.setSensacaoTermica(Math.round(calculada * 100.0) / 100.0);
        }
    }

    public void validarLimitesFisicos(Double temp, Double umi) {
        if (temp == null) {
            throw new IllegalArgumentException(com.app.util.i18n.I18n.get("validacao.temperatura.obrigatoria"));
        }
        if (temp < -90.0 || temp > 65.0) {
            throw new IllegalArgumentException(com.app.util.i18n.I18n.get("validacao.temperatura.invalida", com.app.util.i18n.I18n.formatarNumero(temp, 1)));
        }
        if (umi == null) {
            throw new IllegalArgumentException(com.app.util.i18n.I18n.get("validacao.umidade.obrigatoria"));
        }
        if (umi < 0.0 || umi > 100.0) {
            throw new IllegalArgumentException(com.app.util.i18n.I18n.get("validacao.umidade.invalida", com.app.util.i18n.I18n.formatarNumero(umi, 1)));
        }
    }
}
