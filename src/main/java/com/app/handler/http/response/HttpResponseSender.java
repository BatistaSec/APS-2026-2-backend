package com.app.handler.http.response;

import java.io.IOException;


public interface HttpResponseSender {

    void sucesso(String json) throws IOException;

    void criado(String json) throws IOException;

    void badRequest(String mensagem) throws IOException;

    void naoEncontrado(String mensagem) throws IOException;

    void metodoNaoPermitido() throws IOException;

    void erroInterno(String mensagem) throws IOException;

    void noContent() throws IOException;
}
