package com.app.service.calculo;


public class CalculadoraSensacaoTermicaImpl implements CalculadoraSensacaoTermica {

    @Override
    public double calcular(double tempC, double umidade, double ventoKmH) {
        // Wind Chill para clima frio (<= 10°C) e vento ativo (> 4.8 km/h)
        if (tempC <= 10.0 && ventoKmH > 4.8) {
            double v016 = Math.pow(ventoKmH, 0.16);
            return 13.12 + (0.6215 * tempC) - (11.37 * v016) + (0.3965 * tempC * v016);
        }

        // Heat Index para clima quente e úmido (>= 26°C e umidade >= 40%)
        if (tempC >= 26.0 && umidade >= 40.0) {
            double tF = (tempC * 9.0 / 5.0) + 32.0;
            double r = umidade;

            double hiF = -42.379
                    + (2.04901523 * tF)
                    + (10.14333127 * r)
                    - (0.22475541 * tF * r)
                    - (6.83783e-3 * tF * tF)
                    - (5.481717e-2 * r * r)
                    + (1.22874e-3 * tF * tF * r)
                    + (8.5282e-4 * tF * r * r)
                    - (1.99e-6 * tF * tF * r * r);

            return (hiF - 32.0) * 5.0 / 9.0;
        }

        return tempC;
    }
}
