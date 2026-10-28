package com.app.handler.http.response;

import com.app.model.dto.response.ProblemDetails;

import java.io.IOException;


public interface HttpResponseSender {

    void sucesso(String json) throws IOException;

    void criado(String json) throws IOException;

    void badRequest(String mensagem) throws IOException;

    void naoEncontrado(String mensagem) throws IOException;

    void metodoNaoPermitido() throws IOException;

    void erroInterno(String mensagem) throws IOException;

    void unauthorized(String mensagem) throws IOException;

    void tooManyRequests(String mensagem, long retryAfterSeconds) throws IOException;

    void problema(ProblemDetails problem) throws IOException;

    void noContent() throws IOException;

    void setHeader(String nome, String valor);
}
