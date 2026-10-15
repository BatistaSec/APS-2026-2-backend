package com.app.handler;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.request.HttpRequestImpl;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.http.response.HttpResponseSenderImpl;
import com.app.handler.operacoes.delete.MedicaoDeleteHandler;
import com.app.handler.operacoes.delete.MedicaoDeleteHandlerImpl;
import com.app.handler.operacoes.get.MedicaoGetHandler;
import com.app.handler.operacoes.get.MedicaoGetHandlerImpl;
import com.app.handler.operacoes.post.MedicaoPostHandler;
import com.app.handler.operacoes.post.MedicaoPostHandlerImpl;
import com.app.service.MonitoramentoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;


public class MedicaoApiHandler implements HttpHandler {

    private final MedicaoGetHandler getHandler;
    private final MedicaoPostHandler postHandler;
    private final MedicaoDeleteHandler deleteHandler;

    public MedicaoApiHandler(MonitoramentoService service) {
        this(
                new MedicaoGetHandlerImpl(
                        service.getConsultaDao(),
                        service.getOpenWeatherClient(),
                        service.getRegistroService(),
                        service.getEstatisticaService()
                ),
                new MedicaoPostHandlerImpl(
                        service.getOpenWeatherClient(),
                        service.getRegistroService(),
                        service.getColetorService()
                ),
                new MedicaoDeleteHandlerImpl(
                        service.getDeletarDao()
                )
        );
    }

    public MedicaoApiHandler(MedicaoGetHandler getHandler,
                             MedicaoPostHandler postHandler,
                             MedicaoDeleteHandler deleteHandler) {
        this.getHandler = getHandler;
        this.postHandler = postHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        HttpRequest req = new HttpRequestImpl(exchange);
        HttpResponseSender send = new HttpResponseSenderImpl(exchange);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            send.noContent();
            return;
        }

        try {
            switch (method) {
                case "GET" -> getHandler.tratar(req, send);
                case "POST" -> postHandler.tratar(req, send);
                case "DELETE" -> deleteHandler.tratar(req, send);
                default -> send.metodoNaoPermitido();
            }
        } catch (IllegalArgumentException e) {
            send.badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            send.erroInterno(e.getMessage());
        }
    }
}
