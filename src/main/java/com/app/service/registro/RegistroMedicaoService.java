package com.app.service.registro;

import com.app.model.MedicaoAmbiental;
import com.app.model.MedicaoAmbiental.Origem;

import java.util.List;

public interface RegistroMedicaoService {

    /** Registra uma medição com a origem que a gerou. */
    MedicaoAmbiental registrar(MedicaoAmbiental medicao, Origem origem);

    /** Compatibilidade: usa COLETOR como origem padrão. */
    default MedicaoAmbiental registrar(MedicaoAmbiental medicao) {
        return registrar(medicao, Origem.COLETOR);
    }

    default List<MedicaoAmbiental> registrarLote(List<MedicaoAmbiental> medicoes, Origem origem) {
        if (medicoes == null || medicoes.isEmpty()) return List.of();
        return medicoes.stream().map(m -> registrar(m, origem)).toList();
    }

    default List<MedicaoAmbiental> registrarLote(List<MedicaoAmbiental> medicoes) {
        return registrarLote(medicoes, Origem.SINCRONIZACAO);
    }
}
