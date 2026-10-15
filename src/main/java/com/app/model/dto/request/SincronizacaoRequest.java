package com.app.model.dto.request;

import com.app.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;


public record SincronizacaoRequest(List<String> locais) {

    public static SincronizacaoRequest fromJson(String json) {
        List<String> lista = new ArrayList<>();
        if (json != null && !json.isBlank()) {
            lista.addAll(JsonUtil.extrairListaTexto(json, "cidades"));
            lista.addAll(JsonUtil.extrairListaTexto(json, "locais"));
            lista.addAll(JsonUtil.extrairListaTexto(json, "estados"));

            if (lista.isEmpty()) {
                String unica = JsonUtil.extrairTexto(json, "cidade");
                if (unica == null || unica.isBlank()) {
                    unica = JsonUtil.extrairTexto(json, "estado");
                }
                if (unica != null && !unica.isBlank()) {
                    lista.add(unica);
                }
            }
        }
        return new SincronizacaoRequest(lista);
    }

    public boolean estaVazio() {
        return locais == null || locais.isEmpty();
    }
}
