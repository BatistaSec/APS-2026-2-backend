package com.app.service.registro;

import com.app.dao.salvar.SalvarMedicaoDao;
import com.app.model.MedicaoAmbiental;
import com.app.service.calculo.CalculadoraSensacaoTermica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RegistroMedicaoServiceTest {

    @Test
    @DisplayName("Registro: Deve preencher metadados e calcular sensação térmica antes de persistir")
    void deveRegistrarMedicaoComValidacaoESensacao() {
        SalvarMedicaoDao daoMock = medicao -> {
            medicao.setId(100L);
            return medicao;
        };
        CalculadoraSensacaoTermica calcMock = (t, u, v) -> 35.5;

        RegistroMedicaoService service = new RegistroMedicaoServiceImpl(daoMock, calcMock);

        MedicaoAmbiental m = new MedicaoAmbiental();
        m.setCidade("Rio de Janeiro");
        m.setTemperatura(30.0);
        m.setUmidade(85.0);
        m.setVelocidadeVento(10.0);

        MedicaoAmbiental salva = service.registrar(m);

        assertNotNull(salva.getId(), "ID deve ser gerado pelo DAO");
        assertNotNull(salva.getDataHora(), "Data/hora deve ser preenchida automaticamente");
        assertNotNull(salva.getEstacaoId(), "Estação padrão deve ser gerada");
        assertEquals(35.5, salva.getSensacaoTermica(), "Sensação térmica deve ser calculada pela calculadora");
    }

    @Test
    @DisplayName("Registro: Deve lançar exceção se medição for nula")
    void deveLancarExcecaoParaMedicaoNula() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();
        assertThrows(IllegalArgumentException.class, () -> service.registrar(null));
    }

    @Test
    @DisplayName("Registro Lote: Deve processar e salvar lista de medições")
    void deveRegistrarLoteComSucesso() {
        SalvarMedicaoDao daoMock = new SalvarMedicaoDao() {
            @Override
            public MedicaoAmbiental salvar(MedicaoAmbiental medicao) {
                medicao.setId(1L);
                return medicao;
            }

            @Override
            public java.util.List<MedicaoAmbiental> salvarLote(java.util.List<MedicaoAmbiental> medicoes) {
                long id = 1;
                for (MedicaoAmbiental m : medicoes) {
                    m.setId(id++);
                }
                return medicoes;
            }
        };
        CalculadoraSensacaoTermica calcMock = (t, u, v) -> t;
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl(daoMock, calcMock);

        MedicaoAmbiental m1 = new MedicaoAmbiental();
        m1.setCidade("Curitiba");
        m1.setTemperatura(18.0);
        m1.setUmidade(70.0);

        MedicaoAmbiental m2 = new MedicaoAmbiental();
        m2.setCidade("Tokyo");
        m2.setTemperatura(22.0);
        m2.setUmidade(65.0);

        java.util.List<MedicaoAmbiental> resultado = service.registrarLote(java.util.List.of(m1, m2));

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
        assertEquals(2L, resultado.get(1).getId());
        assertNotNull(resultado.get(0).getEstacaoId());
        assertNotNull(resultado.get(1).getEstacaoId());
    }

    @Test
    @DisplayName("Validação: Deve aceitar valores exatos nos limites físicos (-90°C, 65°C, 0%, 100%)")
    void deveAceitarLimitesFisicosExtremosValidos() {
        SalvarMedicaoDao daoMock = m -> {
            m.setId(999L);
            return m;
        };
        CalculadoraSensacaoTermica calcMock = (t, u, v) -> t;
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl(daoMock, calcMock);

        MedicaoAmbiental min = new MedicaoAmbiental();
        min.setTemperatura(-90.0);
        min.setUmidade(0.0);
        assertDoesNotThrow(() -> service.registrar(min));

        MedicaoAmbiental max = new MedicaoAmbiental();
        max.setTemperatura(65.0);
        max.setUmidade(100.0);
        assertDoesNotThrow(() -> service.registrar(max));
    }

    @Test
    @DisplayName("Validação: Deve lançar IllegalArgumentException para temperatura fora dos limites")
    void deveLancarExcecaoParaTemperaturaForaDosLimites() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();

        MedicaoAmbiental mFrio = new MedicaoAmbiental();
        mFrio.setTemperatura(-90.1);
        mFrio.setUmidade(50.0);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.registrar(mFrio));
        assertTrue(ex1.getMessage().contains("Temperatura inválida"));

        MedicaoAmbiental mQuente = new MedicaoAmbiental();
        mQuente.setTemperatura(65.1);
        mQuente.setUmidade(50.0);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.registrar(mQuente));
        assertTrue(ex2.getMessage().contains("Temperatura inválida"));
    }

    @Test
    @DisplayName("Validação: Deve lançar IllegalArgumentException para umidade fora dos limites")
    void deveLancarExcecaoParaUmidadeForaDosLimites() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();

        MedicaoAmbiental mBaixa = new MedicaoAmbiental();
        mBaixa.setTemperatura(25.0);
        mBaixa.setUmidade(-0.1);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> service.registrar(mBaixa));
        assertTrue(ex1.getMessage().contains("Umidade relativa inválida"));

        MedicaoAmbiental mAlta = new MedicaoAmbiental();
        mAlta.setTemperatura(25.0);
        mAlta.setUmidade(100.1);
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> service.registrar(mAlta));
        assertTrue(ex2.getMessage().contains("Umidade relativa inválida"));
    }

    @Test
    @DisplayName("Validação: Deve lançar IllegalArgumentException para valores nulos de temperatura e umidade")
    void deveLancarExcecaoParaValoresNulos() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();

        MedicaoAmbiental semTemp = new MedicaoAmbiental();
        semTemp.setTemperatura(null);
        semTemp.setUmidade(50.0);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(semTemp));

        MedicaoAmbiental semUmi = new MedicaoAmbiental();
        semUmi.setTemperatura(20.0);
        semUmi.setUmidade(null);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(semUmi));
    }

    @Test
    @DisplayName("Registro Lote: Deve retornar lista vazia para lote vazio ou nulo")
    void deveRetornarListaVaziaParaLoteVazioOuNulo() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();
        assertTrue(service.registrarLote(null).isEmpty());
        assertTrue(service.registrarLote(java.util.List.of()).isEmpty());
    }

    @Test
    @DisplayName("Registro Lote: Deve interromper processamento e lançar exceção se qualquer item for inválido")
    void deveInterromperLoteQuandoUmElementoForInvalido() {
        RegistroMedicaoService service = new RegistroMedicaoServiceImpl();

        MedicaoAmbiental valida = new MedicaoAmbiental();
        valida.setTemperatura(20.0);
        valida.setUmidade(50.0);

        MedicaoAmbiental invalida = new MedicaoAmbiental();
        invalida.setTemperatura(80.0); // Fora do limite máximo (65°C)
        invalida.setUmidade(50.0);

        assertThrows(IllegalArgumentException.class, () -> service.registrarLote(java.util.List.of(valida, invalida)));
    }
}
