package com.app.service.registro;

import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.model.MedicaoAmbiental;
import com.app.service.calculo.CalculadoraSensacaoTermica;
import com.app.service.calculo.CalculadoraSensacaoTermicaImpl;

import java.time.LocalDateTime;


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
        if (medicao == null) {
            throw new IllegalArgumentException("A medição ambiental não pode ser nula.");
        }

        if (medicao.getDataHora() == null) {
            medicao.setDataHora(LocalDateTime.now());
        }

        if (medicao.getEstacaoId() == null || medicao.getEstacaoId().isBlank()) {
            String cid = medicao.getCidade() != null ? medicao.getCidade().toUpperCase().replace(" ", "-") : "PADRAO";
            medicao.setEstacaoId("ESTACAO-" + cid);
        }

        // Calcula sensação térmica se não informada
        if (medicao.getSensacaoTermica() == null && medicao.getTemperatura() != null) {
            double temp = medicao.getTemperatura();
            double umi = medicao.getUmidade() != null ? medicao.getUmidade() : 50.0;
            double vento = medicao.getVelocidadeVento() != null ? medicao.getVelocidadeVento() : 0.0;
            double calculada = calculadoraSensacaoTermica.calcular(temp, umi, vento);
            medicao.setSensacaoTermica(Math.round(calculada * 100.0) / 100.0);
        }

        return salvarMedicaoDao.salvar(medicao);
    }
}
