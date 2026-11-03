package com.app.service.registro;

import com.app.dao.EstacaoRepository;
import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.model.Cidade;
import com.app.model.Estacao;
import com.app.model.MedicaoAmbiental;
import com.app.model.MedicaoAmbiental.Origem;
import com.app.service.calculo.CalculadoraSensacaoTermica;
import com.app.service.calculo.CalculadoraSensacaoTermicaImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class RegistroMedicaoServiceImpl implements RegistroMedicaoService {

    private final SalvarMedicaoDao salvarMedicaoDao;
    private final CalculadoraSensacaoTermica calculadoraSensacaoTermica;
    private final EstacaoRepository estacaoRepository;

    public RegistroMedicaoServiceImpl() {
        this(new SalvarMedicaoDaoImpl(), new CalculadoraSensacaoTermicaImpl(), EstacaoRepository.getInstance());
    }

    public RegistroMedicaoServiceImpl(SalvarMedicaoDao salvarMedicaoDao,
                                      CalculadoraSensacaoTermica calculadoraSensacaoTermica,
                                      EstacaoRepository estacaoRepository) {
        this.salvarMedicaoDao = salvarMedicaoDao;
        this.calculadoraSensacaoTermica = calculadoraSensacaoTermica;
        this.estacaoRepository = estacaoRepository;
    }

    @Override
    public MedicaoAmbiental registrar(MedicaoAmbiental medicao, Origem origem) {
        prepararMedicao(medicao, origem);
        return salvarMedicaoDao.salvar(medicao);
    }

    @Override
    public List<MedicaoAmbiental> registrarLote(List<MedicaoAmbiental> medicoes, Origem origem) {
        if (medicoes == null || medicoes.isEmpty()) return List.of();
        for (MedicaoAmbiental m : medicoes) prepararMedicao(m, origem);
        return salvarMedicaoDao.salvarLote(medicoes);
    }

    // ── lógica de preparação ───────────────────────────────────────────────

    public void prepararMedicao(MedicaoAmbiental medicao, Origem origem) {
        if (medicao == null) {
            throw new IllegalArgumentException("A medição ambiental não pode ser nula.");
        }

        validarLimitesFisicos(medicao.getTemperatura(), medicao.getUmidade());

        // Define origem
        if (origem != null) {
            medicao.setOrigem(origem);
        } else if (medicao.getOrigem() == null) {
            medicao.setOrigem(Origem.MANUAL);
        }

        // Resolve estação/cidade se ainda não vinculada
        if (medicao.getEstacao() == null) {
            resolverEstacao(medicao);
        }

        // Defaults de campos obrigatórios
        if (medicao.getDataHora() == null) {
            medicao.setDataHora(LocalDateTime.now());
        }
        if (medicao.getPrecipitacao() == null) {
            medicao.setPrecipitacao(false);
        }
        if (medicao.getVelocidadeVento() == null) {
            medicao.setVelocidadeVento(0.0);
        }

        // Calcula sensação térmica se ausente
        if (medicao.getSensacaoTermica() == null && medicao.getTemperatura() != null) {
            double temp  = medicao.getTemperatura();
            double umi   = medicao.getUmidade()       != null ? medicao.getUmidade()       : 50.0;
            double vento = medicao.getVelocidadeVento() != null ? medicao.getVelocidadeVento() : 0.0;
            double calc  = calculadoraSensacaoTermica.calcular(temp, umi, vento);
            medicao.setSensacaoTermica(Math.round(calc * 100.0) / 100.0);
        }
    }

    /**
     * Resolve ou cria a Estacao (e sua Cidade) para uma medição que ainda não tem vínculo.
     * Usa campos herdados do objeto OpenWeather (cidade já preenchida via setter temporário).
     */
    private void resolverEstacao(MedicaoAmbiental medicao) {
        // O OpenWeatherClientImpl seta o estacaoId temporariamente como String via campo helper
        // Aqui lemos o estacaoId que foi injetado como campo transiente antes de chamar registrar()
        String estacaoIdStr   = medicao.getEstacaoIdTemp();
        String nomeCidade     = medicao.getCidadeTemp();
        String estadoCidade   = medicao.getEstadoTemp();
        String paisCidade     = medicao.getPaisTemp();
        Estacao.Tipo tipo     = (estacaoIdStr != null && estacaoIdStr.startsWith("OPENWEATHER"))
                                ? Estacao.Tipo.OWM : Estacao.Tipo.MANUAL;

        Cidade cidade = null;
        if (nomeCidade != null && !nomeCidade.isBlank()) {
            cidade = estacaoRepository.buscarOuCriarCidade(nomeCidade, estadoCidade, paisCidade);
        }

        String idEstacao = estacaoIdStr != null ? estacaoIdStr : "ESTACAO-MANUAL";
        Estacao estacao  = estacaoRepository.buscarOuCriarEstacao(idEstacao, cidade, tipo);
        medicao.setEstacao(estacao);
    }

    public void validarLimitesFisicos(Double temp, Double umi) {
        if (temp == null) {
            throw new IllegalArgumentException("A temperatura é de preenchimento obrigatório.");
        }
        if (temp < -90.0 || temp > 65.0) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Temperatura inválida (%.1f °C). O valor deve estar entre -90,0 °C e 65,0 °C.", temp));
        }
        if (umi == null) {
            throw new IllegalArgumentException("A umidade relativa é de preenchimento obrigatório.");
        }
        if (umi < 0.0 || umi > 100.0) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "Umidade relativa inválida (%.1f%%). O valor deve estar entre 0,0%% e 100,0%%.", umi));
        }
    }
}
