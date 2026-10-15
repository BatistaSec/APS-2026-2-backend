package com.app.handler.operacoes.get;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;

import java.io.IOException;


public interface MedicaoGetHandler {

    void tratar(HttpRequest req, HttpResponseSender send) throws IOException;
}
