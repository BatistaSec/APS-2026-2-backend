package com.app.handler.http.request;


public interface HttpRequest {

    String getMethod();

    String getPath();

    String getQueryParam(String nome);

    int getQueryParamInt(String nome, int padrao);

    String getBody();

    Long getIdFromPath();
}
