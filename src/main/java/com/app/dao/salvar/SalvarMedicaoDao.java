package com.app.dao.salvar;

import com.app.model.MedicaoAmbiental;

import java.util.List;

public interface SalvarMedicaoDao {

    MedicaoAmbiental salvar(MedicaoAmbiental medicao);

    default List<MedicaoAmbiental> salvarLote(List<MedicaoAmbiental> medicoes) {
        if (medicoes == null) return List.of();
        return medicoes.stream().map(this::salvar).toList();
    }
}
