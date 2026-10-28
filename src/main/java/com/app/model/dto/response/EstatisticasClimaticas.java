package com.app.model.dto.response;

import java.util.Locale;


public record EstatisticasClimaticas(
        int totalAnalisadas,
        double tempMin,
        double tempMax,
        double tempMedia,
        double umidadeMin,
        double umidadeMax,
        double umidadeMedia,
        double ventoMaximo,
        long ocorrenciasChuva
) {

    public String toJson() {
        if (totalAnalisadas == 0) {
            return "{\"total\":0,\"mensagem\":\"Nenhuma medição disponível até o momento.\"}";
        }

        return String.format(Locale.US,
                "{" +
                "\"totalAnalisadas\":%d," +
                "\"temperatura\":{\"min\":%.2f,\"max\":%.2f,\"media\":%.2f}," +
                "\"umidade\":{\"min\":%.2f,\"max\":%.2f,\"media\":%.2f}," +
                "\"ventoMaximo\":%.2f," +
                "\"ocorrenciasChuva\":%d" +
                "}",
                totalAnalisadas,
                tempMin, tempMax, tempMedia,
                umidadeMin, umidadeMax, umidadeMedia,
                ventoMaximo,
                ocorrenciasChuva
        );
    }
}
