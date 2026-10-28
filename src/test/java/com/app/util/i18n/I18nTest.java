package com.app.util.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class I18nTest {

    @Test
    @DisplayName("I18n: Deve recuperar mensagens do ResourceBundle em pt-BR com argumentos formatados")
    void deveRecuperarMensagensTraduzidas() {
        String msgValidacao = I18n.get("validacao.temperatura.obrigatoria");
        assertEquals("A temperatura é de preenchimento obrigatório.", msgValidacao);

        String msgTituloErro = I18n.get("erro.bad_request.titulo");
        assertEquals("Requisição Inválida", msgTituloErro);

        String msgComParametros = I18n.get("erro.metodo_nao_permitido.detalhe", "PUT");
        assertEquals("O método HTTP PUT não é permitido para este endpoint.", msgComParametros);

        String msgSucesso = I18n.get("sucesso.coleta.adicionada", "Curitiba");
        assertEquals("Localização 'Curitiba' adicionada à coleta automática com sucesso.", msgSucesso);
    }

    @Test
    @DisplayName("I18n: Deve formatar datas e horários no padrão regional brasileiro dd/MM/yyyy 24h")
    void deveFormatarDatasNoPadraoBrasileiro() {
        LocalDateTime dt = LocalDateTime.of(2026, 10, 19, 14, 30, 45);

        assertEquals("19/10/2026", I18n.formatarData(dt));
        assertEquals("19/10/2026 14:30:45", I18n.formatarDataHora(dt));
        assertEquals("", I18n.formatarData(null));
        assertEquals("", I18n.formatarDataHora(null));
    }

    @Test
    @DisplayName("I18n: Deve formatar números com vírgula decimal e ponto de milhar no padrão pt-BR")
    void deveFormatarNumerosComVirgula() {
        String formatado = I18n.formatarNumero(1234.56, 2);
        assertTrue(formatado.contains(",") && formatado.contains("."), "Deve ter vírgula decimal e ponto de milhar: " + formatado);
        assertEquals("1.234,56", formatado);

        String formatado1Casa = I18n.formatarNumero(21.5, 1);
        assertEquals("21,5", formatado1Casa);
    }

    @Test
    @DisplayName("I18n: Deve formatar unidades ambientais (temperatura, umidade, vento, pressão)")
    void deveFormatarUnidadesAmbientais() {
        assertEquals("22,5 °C", I18n.formatarTemperatura(22.5));
        assertEquals("65,0%", I18n.formatarUmidade(65.0));
        assertEquals("14,2 km/h", I18n.formatarVento(14.2));
        assertEquals("1013,3 hPa", I18n.formatarPressao(1013.25));
    }

    @Test
    @DisplayName("I18n: Deve formatar valores monetários em Real Brasileiro (BRL / R$)")
    void deveFormatarMoedaBrasileira() {
        String moeda = I18n.formatarMoeda(150.0);
        assertTrue(moeda.contains("R$") && moeda.contains("150,00"), "Deve conter 'R$' e '150,00': " + moeda);
    }
}
