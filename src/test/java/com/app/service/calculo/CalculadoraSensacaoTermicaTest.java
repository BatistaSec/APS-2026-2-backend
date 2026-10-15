package com.app.service.calculo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CalculadoraSensacaoTermicaTest {

    private final CalculadoraSensacaoTermica calculadora = new CalculadoraSensacaoTermicaImpl();

    @Test
    @DisplayName("Cálculo: Deve retornar a própria temperatura em clima ameno")
    void deveRetornarPropriaTemperaturaEmClimaAmeno() {
        double sensacao = calculadora.calcular(22.0, 50.0, 10.0);
        assertEquals(22.0, sensacao, 0.01);
    }

    @Test
    @DisplayName("Cálculo: Deve calcular resfriamento do vento (Wind Chill) no frio com vento")
    void deveCalcularWindChillParaClimaFrio() {
        double sensacao = calculadora.calcular(5.0, 60.0, 20.0);
        assertTrue(sensacao < 5.0, "Sensação no frio com vento deve ser menor que a temperatura real");
    }

    @Test
    @DisplayName("Cálculo: Deve calcular índice de calor (Heat Index) no calor com alta umidade")
    void deveCalcularHeatIndexParaClimaQuenteEUmid() {
        double sensacao = calculadora.calcular(32.0, 80.0, 5.0);
        assertTrue(sensacao > 32.0, "Sensação no calor com umidade deve ser maior que a temperatura real");
    }
}
