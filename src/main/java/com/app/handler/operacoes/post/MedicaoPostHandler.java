package com.app.handler.operacoes.post;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;

import java.io.IOException;


public interface MedicaoPostHandler {

    void tratar(HttpRequest req, HttpResponseSender send) throws IOException;
}
