package com.app.util.i18n;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utilitário central de Internacionalização (i18n) e Localização (l10n).
 * Padronizado para Português Brasileiro (pt-BR).
 */
public final class I18n {

    public static final Locale LOCALE_PADRAO = Locale.of("pt", "BR");
    public static final DateTimeFormatter FORMATADOR_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATADOR_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String BUNDLE_BASE_NAME = "messages";

    private I18n() {}

    /**
     * Obtém mensagem internacionalizada pelo identificador (chave), formatando argumentos se houver.
     */
    public static String get(String chave, Object... args) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_PADRAO);
            if (bundle.containsKey(chave)) {
                String pattern = bundle.getString(chave);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {}
        return chave;
    }

    /**
     * Formata data no padrão brasileiro: dd/MM/yyyy
     */
    public static String formatarData(LocalDateTime dataHora) {
        if (dataHora == null) return "";
        return dataHora.format(FORMATADOR_DATA);
    }

    /**
     * Formata data e horário no padrão brasileiro 24h: dd/MM/yyyy HH:mm:ss
     */
    public static String formatarDataHora(LocalDateTime dataHora) {
        if (dataHora == null) return "";
        return dataHora.format(FORMATADOR_DATA_HORA);
    }

    /**
     * Formata números com separador decimal com vírgula e milhar com ponto.
     */
    public static String formatarNumero(double valor, int casasDecimais) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_PADRAO);
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setMinimumFractionDigits(casasDecimais);
        df.setMaximumFractionDigits(casasDecimais);
        df.setGroupingUsed(true);
        return df.format(valor);
    }

    /**
     * Formata temperatura em Celsius no padrão pt-BR: ex. "21,5 °C"
     */
    public static String formatarTemperatura(double temp) {
        return String.format(LOCALE_PADRAO, "%.1f °C", temp);
    }

    /**
     * Formata percentual de umidade no padrão pt-BR: ex. "65,0%"
     */
    public static String formatarUmidade(double umidade) {
        return String.format(LOCALE_PADRAO, "%.1f%%", umidade);
    }

    /**
     * Formata velocidade do vento em km/h no padrão pt-BR: ex. "14,2 km/h"
     */
    public static String formatarVento(double ventoKmH) {
        return String.format(LOCALE_PADRAO, "%.1f km/h", ventoKmH);
    }

    /**
     * Formata pressão atmosférica em hPa no padrão pt-BR: ex. "1.013,2 hPa"
     */
    public static String formatarPressao(double pressaoHpa) {
        return String.format(LOCALE_PADRAO, "%.1f hPa", pressaoHpa);
    }

    /**
     * Formata valor monetário no padrão Real Brasileiro (BRL / R$): ex. "R$ 150,00"
     */
    public static String formatarMoeda(double valor) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_PADRAO);
        return nf.format(valor);
    }
}
