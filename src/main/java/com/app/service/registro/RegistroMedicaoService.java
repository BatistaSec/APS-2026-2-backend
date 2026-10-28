package com.app.service.registro;

import com.app.model.MedicaoAmbiental;

import java.util.List;

public interface RegistroMedicaoService {

    MedicaoAmbiental registrar(MedicaoAmbiental medicao);

    default List<MedicaoAmbiental> registrarLote(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null || medicoes.isEmpty()) return List.of();
        return medicoes.stream().map(this::registrar).toList();
    }
}
