package com.app.model.dto.request;

import com.app.util.JsonUtil;

import java.time.LocalDateTime;
import java.util.Map;


public record MedicaoRequest(
        String cidade,
        String estado,
        String pais,
        LocalDateTime dataHora,
        Double temperatura,
        Double umidade,
        Boolean precipitacao,
        Double velocidadeVento,
        Double pressaoAtmosferica,
        String direcaoVento,
        Double indiceUv
) {

    private static final Map<String, String> CAPITAIS_POR_ESTADO = Map.ofEntries(
            Map.entry("AC", "Rio Branco"),
            Map.entry("AL", "Maceio"),
            Map.entry("AP", "Macapa"),
            Map.entry("AM", "Manaus"),
            Map.entry("BA", "Salvador"),
            Map.entry("CE", "Fortaleza"),
            Map.entry("DF", "Brasilia"),
            Map.entry("ES", "Vitoria"),
            Map.entry("GO", "Goiania"),
            Map.entry("MA", "Sao Luis"),
            Map.entry("MT", "Cuiaba"),
            Map.entry("MS", "Campo Grande"),
            Map.entry("MG", "Belo Horizonte"),
            Map.entry("PA", "Belem"),
            Map.entry("PB", "Joao Pessoa"),
            Map.entry("PR", "Curitiba"),
            Map.entry("PE", "Recife"),
            Map.entry("PI", "Teresina"),
            Map.entry("RJ", "Rio de Janeiro"),
            Map.entry("RN", "Natal"),
            Map.entry("RS", "Porto Alegre"),
            Map.entry("RO", "Porto Velho"),
            Map.entry("RR", "Boa Vista"),
            Map.entry("SC", "Florianopolis"),
            Map.entry("SP", "Sao Paulo"),
            Map.entry("SE", "Aracaju"),
            Map.entry("TO", "Palmas")
    );

    public static MedicaoRequest fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new MedicaoRequest(null, null, null, null, null, null, null, null, null, null, null);
        }

        String cid = JsonUtil.extrairTexto(json, "cidade");
        String est = JsonUtil.extrairTexto(json, "estado");
        String p = JsonUtil.extrairTexto(json, "pais");

        String dataHoraTxt = JsonUtil.extrairTexto(json, "dataHora");
        LocalDateTime dt = null;
        if (dataHoraTxt != null && !dataHoraTxt.isBlank()) {
            try {
                dt = LocalDateTime.parse(dataHoraTxt);
            } catch (Exception ignored) {}
        }

        Double temp = JsonUtil.extrairNumero(json, "temperatura");
        Double umi = JsonUtil.extrairNumero(json, "umidade");
        Boolean prec = JsonUtil.extrairBooleano(json, "precipitacao");
        Double vento = JsonUtil.extrairNumero(json, "velocidadeVento");
        Double pressao = JsonUtil.extrairNumero(json, "pressaoAtmosferica");
        String dirVento = JsonUtil.extrairTexto(json, "direcaoVento");
        Double uv = JsonUtil.extrairNumero(json, "indiceUv");

        return new MedicaoRequest(cid, est, p, dt, temp, umi, prec, vento, pressao, dirVento, uv);
    }


    public String resolverLocalizacaoConsulta() {
        String cid = (cidade != null && !cidade.isBlank()) ? cidade.trim() : null;
        String est = (estado != null && !estado.isBlank()) ? estado.trim() : null;
        String ps = (pais != null && !pais.isBlank()) ? pais.trim() : null;

        if (cid != null && est != null) {
            return cid + "," + est + "," + (ps != null ? ps : "BR");
        }

        if (cid != null) {
            return ps != null ? cid + "," + ps : cid;
        }

        if (est != null) {
            String estUpper = est.toUpperCase();
            if (CAPITAIS_POR_ESTADO.containsKey(estUpper)) {
                return CAPITAIS_POR_ESTADO.get(estUpper) + ",BR";
            }
            return est + (ps != null ? "," + ps : ",BR");
        }

        return null;
    }

    public boolean temLocalizacao() {
        return (cidade != null && !cidade.isBlank()) || (estado != null && !estado.isBlank());
    }
}
