package com.app.service;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.dao.consulta.ConsultaMedicaoDaoImpl;
import com.app.dao.exclusao.DeletarMedicaoDao;
import com.app.dao.exclusao.DeletarMedicaoDaoImpl;
import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.dao.salvar.SalvarMedicaoDaoImpl;
import com.app.service.calculo.CalculadoraSensacaoTermica;
import com.app.service.calculo.CalculadoraSensacaoTermicaImpl;
import com.app.service.coleta.ColetorAgendadoService;
import com.app.service.coleta.ColetorAgendadoServiceImpl;
import com.app.service.estatistica.EstatisticaService;
import com.app.service.estatistica.EstatisticaServiceImpl;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.openweather.OpenWeatherClientImpl;
import com.app.service.registro.RegistroMedicaoService;
import com.app.service.registro.RegistroMedicaoServiceImpl;


public class MonitoramentoService {

    private final ConsultaMedicaoDao consultaDao;
    private final SalvarMedicaoDao salvarDao;
    private final DeletarMedicaoDao deletarDao;
    private final RegistroMedicaoService registroService;
    private final EstatisticaService estatisticaService;
    private final OpenWeatherClient openWeatherClient;
    private final ColetorAgendadoService coletorService;

    public MonitoramentoService() {
        this(
                new ConsultaMedicaoDaoImpl(),
                new SalvarMedicaoDaoImpl(),
                new DeletarMedicaoDaoImpl(),
                new CalculadoraSensacaoTermicaImpl(),
                new OpenWeatherClientImpl()
        );
    }

    public MonitoramentoService(ConsultaMedicaoDao consultaDao, SalvarMedicaoDao salvarDao,
                                DeletarMedicaoDao deletarDao, CalculadoraSensacaoTermica calculadora,
                                OpenWeatherClient openWeatherClient) {
        this.consultaDao = consultaDao;
        this.salvarDao = salvarDao;
        this.deletarDao = deletarDao;
        this.openWeatherClient = openWeatherClient;
        this.registroService = new RegistroMedicaoServiceImpl(salvarDao, calculadora);
        this.estatisticaService = new EstatisticaServiceImpl(consultaDao);
        this.coletorService = new ColetorAgendadoServiceImpl(openWeatherClient, this.registroService);
    }

    public ConsultaMedicaoDao getConsultaDao() {
        return consultaDao;
    }

    public SalvarMedicaoDao getSalvarDao() {
        return salvarDao;
    }

    public DeletarMedicaoDao getDeletarDao() {
        return deletarDao;
    }

    public RegistroMedicaoService getRegistroService() {
        return registroService;
    }

    public EstatisticaService getEstatisticaService() {
        return estatisticaService;
    }

    public OpenWeatherClient getOpenWeatherClient() {
        return openWeatherClient;
    }

    public ColetorAgendadoService getColetorService() {
        return coletorService;
    }
}
