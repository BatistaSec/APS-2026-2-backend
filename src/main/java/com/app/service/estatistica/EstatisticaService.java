package com.app.service.estatistica;

import com.app.model.dto.response.EstatisticasClimaticas;


public interface EstatisticaService {

    EstatisticasClimaticas calcularEstatisticasRecentes(int limite);


    EstatisticasClimaticas calcularEstatisticasPorCidade(String cidade, int limite);
}
