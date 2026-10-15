package com.app.handler.operacoes.delete;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;

import java.io.IOException;


public interface MedicaoDeleteHandler {

    void tratar(HttpRequest req, HttpResponseSender send) throws IOException;
}
