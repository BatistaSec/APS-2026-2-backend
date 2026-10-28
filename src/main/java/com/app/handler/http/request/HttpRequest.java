package com.app.handler.http.request;


public interface HttpRequest {

    String getMethod();

    String getPath();

    String getQueryParam(String nome);

    int getQueryParamInt(String nome, int padrao);

    Double getQueryParamDouble(String nome, Double padrao);

    Boolean getQueryParamBoolean(String nome, Boolean padrao);

    String getHeader(String nome);

    String getClientIp();

    String getBody();

    Long getIdFromPath();
}
