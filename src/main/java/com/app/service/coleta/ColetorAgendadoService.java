package com.app.service.coleta;

import java.util.List;


public interface ColetorAgendadoService {

    void iniciar();

    void parar();

    void executarCicloColeta();

    void adicionarCidade(String cidade);

    boolean removerCidade(String cidade);

    List<String> getCidades();
}
