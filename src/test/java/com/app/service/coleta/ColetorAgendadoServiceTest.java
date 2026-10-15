package com.app.service.coleta;

import com.app.model.MedicaoAmbiental;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.registro.RegistroMedicaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ColetorAgendadoServiceTest {

    @Test
    @DisplayName("Coletor: Deve executar ciclo de coleta e delegar ao serviço de registro")
    void deveExecutarCicloDeColetaComSucesso() {
        AtomicBoolean registrado = new AtomicBoolean(false);

        RegistroMedicaoService registroMock = m -> {
            registrado.set(true);
            m.setId(123L);
            return m;
        };

        OpenWeatherClient clientMock = new OpenWeatherClient() {
            @Override
            public MedicaoAmbiental buscarClima(String cidade) {
                MedicaoAmbiental m = new MedicaoAmbiental();
                m.setCidade(cidade);
                m.setTemperatura(20.0);
                m.setUmidade(50.0);
                return m;
            }

            @Override
            public String getApiKey() {
                return "test-key";
            }
        };

        ColetorAgendadoService coletor = new ColetorAgendadoServiceImpl(clientMock, registroMock, List.of("Curitiba,BR"));
        coletor.executarCicloColeta();

        assertTrue(registrado.get(), "O ciclo do coletor deve registrar os dados do clima");
    }

    @Test
    @DisplayName("Coletor: Deve gerenciar pool dinâmico de cidades")
    void deveGerenciarPoolDinamicoDeCidades() {
        ColetorAgendadoService coletor = new ColetorAgendadoServiceImpl();

        int inicial = coletor.getCidades().size();
        coletor.adicionarCidade("Paris,FR");
        assertEquals(inicial + 1, coletor.getCidades().size());
        assertTrue(coletor.getCidades().contains("Paris,FR"));

        boolean removido = coletor.removerCidade("Paris,FR");
        assertTrue(removido);
        assertEquals(inicial, coletor.getCidades().size());
    }
}
