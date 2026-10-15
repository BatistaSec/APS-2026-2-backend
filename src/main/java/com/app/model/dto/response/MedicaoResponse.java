package com.app.model.dto.response;

import com.app.model.MedicaoAmbiental;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public record MedicaoResponse(
        Long id,
        String estacaoId,
        String cidade,
        String dataHora,
        Double temperatura,
        Double umidade,
        Boolean precipitacao,
        Double velocidadeVento,
        Double pressaoAtmosferica,
        String direcaoVento,
        Double indiceUv,
        Double sensacaoTermica
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static MedicaoResponse fromEntity(MedicaoAmbiental entity) {
        if (entity == null) {
            return null;
        }

        String dataHoraFormatada = entity.getDataHora() != null
                ? entity.getDataHora().format(ISO_FORMATTER)
                : "";

        return new MedicaoResponse(
                entity.getId(),
                entity.getEstacaoId(),
                entity.getCidade(),
                dataHoraFormatada,
                entity.getTemperatura(),
                entity.getUmidade(),
                entity.getPrecipitacao(),
                entity.getVelocidadeVento(),
                entity.getPressaoAtmosferica(),
                entity.getDirecaoVento(),
                entity.getIndiceUv(),
                entity.getSensacaoTermica()
        );
    }

    public String toJson() {
        return String.format(Locale.US,
                "{" +
                "\"id\":%d," +
                "\"estacaoId\":\"%s\"," +
                "\"cidade\":\"%s\"," +
                "\"dataHora\":\"%s\"," +
                "\"temperatura\":%.2f," +
                "\"umidade\":%.2f," +
                "\"precipitacao\":%b," +
                "\"velocidadeVento\":%.2f," +
                "\"pressaoAtmosferica\":%.2f," +
                "\"direcaoVento\":\"%s\"," +
                "\"indiceUv\":%.2f," +
                "\"sensacaoTermica\":%.2f" +
                "}",
                id != null ? id : 0,
                estacaoId != null ? estacaoId : "",
                cidade != null ? cidade : "",
                dataHora != null ? dataHora : "",
                temperatura != null ? temperatura : 0.0,
                umidade != null ? umidade : 0.0,
                precipitacao != null ? precipitacao : false,
                velocidadeVento != null ? velocidadeVento : 0.0,
                pressaoAtmosferica != null ? pressaoAtmosferica : 0.0,
                direcaoVento != null ? direcaoVento : "N",
                indiceUv != null ? indiceUv : 0.0,
                sensacaoTermica != null ? sensacaoTermica : 0.0
        );
    }

    public static String fromEntitiesToJson(List<MedicaoAmbiental> entidades) {
        if (entidades == null || entidades.isEmpty()) {
            return "[]";
        }
        return "[" + entidades.stream()
                .map(MedicaoResponse::fromEntity)
                .map(MedicaoResponse::toJson)
                .collect(Collectors.joining(",")) + "]";
    }
}
