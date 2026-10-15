package com.app.service.openweather;

import com.app.model.MedicaoAmbiental;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OpenWeatherClientTest {

    private final OpenWeatherClientImpl client = new OpenWeatherClientImpl();

    @Test
    @DisplayName("OpenWeather: Deve converter graus do vento para rosa dos ventos")
    void deveConverterGrausParaRosa() {
        assertEquals("N", OpenWeatherClientImpl.converterGrausParaRosa(0));
        assertEquals("N", OpenWeatherClientImpl.converterGrausParaRosa(360));
        assertEquals("N", OpenWeatherClientImpl.converterGrausParaRosa(355));
        assertEquals("NE", OpenWeatherClientImpl.converterGrausParaRosa(45));
        assertEquals("L", OpenWeatherClientImpl.converterGrausParaRosa(90));
        assertEquals("SE", OpenWeatherClientImpl.converterGrausParaRosa(135));
        assertEquals("S", OpenWeatherClientImpl.converterGrausParaRosa(180));
        assertEquals("SO", OpenWeatherClientImpl.converterGrausParaRosa(225));
        assertEquals("O", OpenWeatherClientImpl.converterGrausParaRosa(270));
        assertEquals("NO", OpenWeatherClientImpl.converterGrausParaRosa(315));
        assertEquals("NO", OpenWeatherClientImpl.converterGrausParaRosa(320));
    }

    @Test
    @DisplayName("OpenWeather: Deve converter JSON em MedicaoAmbiental com precisão")
    void deveConverterJsonParaMedicao() {
        String mockJson = """
        {
          "coord": {"lon": -46.6361, "lat": -23.5475},
          "weather": [{"id": 501, "main": "Rain", "description": "chuva moderada", "icon": "10n"}],
          "main": {
            "temp": 21.5,
            "feels_like": 22.0,
            "pressure": 1014.0,
            "humidity": 85.0
          },
          "wind": {
            "speed": 5.0,
            "deg": 180
          },
          "rain": {"1h": 2.5},
          "clouds": {"all": 90},
          "dt": 1700000000,
          "name": "São Paulo"
        }
        """;

        MedicaoAmbiental medicao = client.converterJsonParaMedicao(mockJson);

        assertNotNull(medicao);
        assertEquals("OPENWEATHER-SAO-PAULO", medicao.getEstacaoId());
        assertEquals("São Paulo", medicao.getCidade());
        assertEquals(21.5, medicao.getTemperatura());
        assertEquals(22.0, medicao.getSensacaoTermica());
        assertEquals(85.0, medicao.getUmidade());
        assertEquals(1014.0, medicao.getPressaoAtmosferica());
        assertEquals(18.0, medicao.getVelocidadeVento());
        assertEquals("S", medicao.getDirecaoVento());
        assertTrue(medicao.getPrecipitacao(), "Precipitação deve ser detectada");
        assertNotNull(medicao.getDataHora());
    }

    @Test
    @DisplayName("OpenWeather: Deve consumir a API real para qualquer cidade informada")
    void deveConsumirApiRealParaQualquerCidade() {
        assertDoesNotThrow(() -> {
            MedicaoAmbiental medicao = client.buscarClima("Tokyo,JP");
            assertNotNull(medicao);
            assertTrue(medicao.getCidade().equalsIgnoreCase("Tokyo"));
            assertNotNull(medicao.getTemperatura());
            assertNotNull(medicao.getUmidade());
        });
    }
}
