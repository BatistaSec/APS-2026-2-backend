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
}
