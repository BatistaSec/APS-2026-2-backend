package com.app.service.openweather;

import com.app.model.MedicaoAmbiental;


public interface OpenWeatherClient {


    MedicaoAmbiental buscarClima(String cidade);


    String getApiKey();
}
