package com.app.model.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MedicaoRequestTest {

    @Test
    @DisplayName("DTO Request: Deve resolver consulta por cidade e estado")
    void deveResolverCidadeEEstado() {
        MedicaoRequest req = new MedicaoRequest("Curitiba", "PR", null, null, null, null, null, null, null, null, null);
        assertEquals("Curitiba,PR,BR", req.resolverLocalizacaoConsulta());
    }

    @Test
    @DisplayName("DTO Request: Deve resolver capital ao informar apenas o estado")
    void deveResolverApenasEstado() {
        MedicaoRequest reqSp = new MedicaoRequest(null, "SP", null, null, null, null, null, null, null, null, null);
        assertEquals("Sao Paulo,BR", reqSp.resolverLocalizacaoConsulta());

        MedicaoRequest reqRj = new MedicaoRequest(null, "RJ", null, null, null, null, null, null, null, null, null);
        assertEquals("Rio de Janeiro,BR", reqRj.resolverLocalizacaoConsulta());
    }

    @Test
    @DisplayName("DTO Request: Deve fazer parsing de JSON simplificado apenas com estado")
    void deveFazerParsingJsonApenasComEstado() {
        String json = "{\"estado\": \"PR\"}";
        MedicaoRequest req = MedicaoRequest.fromJson(json);

        assertNotNull(req);
        assertEquals("PR", req.estado());
        assertTrue(req.temLocalizacao());
        assertEquals("Curitiba,BR", req.resolverLocalizacaoConsulta());
    }

    @Test
    @DisplayName("DTO Request: Deve fazer parsing de JSON com cidade, data e overrides")
    void deveFazerParsingJsonCompleto() {
        String json = """
        {
          "cidade": "Curitiba",
          "estado": "PR",
          "dataHora": "2026-10-14T20:30:00",
          "temperatura": 19.5
        }
        """;
        MedicaoRequest req = MedicaoRequest.fromJson(json);

        assertNotNull(req);
        assertEquals("Curitiba", req.cidade());
        assertEquals("PR", req.estado());
        assertEquals(19.5, req.temperatura());
        assertNotNull(req.dataHora());
    }
}
