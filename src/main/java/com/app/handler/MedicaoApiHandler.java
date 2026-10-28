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
import com.app.handler.security.RateLimiter;
import com.app.service.MonitoramentoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class MedicaoApiHandler implements HttpHandler {

    private final MedicaoGetHandler getHandler;
    private final MedicaoPostHandler postHandler;
    private final MedicaoDeleteHandler deleteHandler;
    private final RateLimiter rateLimiter;

    public MedicaoApiHandler(MonitoramentoService service) {
        this(service, new RateLimiter());
    }

    public MedicaoApiHandler(MonitoramentoService service, RateLimiter rateLimiter) {
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
                ),
                rateLimiter
        );
    }

    public MedicaoApiHandler(MedicaoGetHandler getHandler,
                             MedicaoPostHandler postHandler,
                             MedicaoDeleteHandler deleteHandler) {
        this(getHandler, postHandler, deleteHandler, new RateLimiter());
    }

    public MedicaoApiHandler(MedicaoGetHandler getHandler,
                             MedicaoPostHandler postHandler,
                             MedicaoDeleteHandler deleteHandler,
                             RateLimiter rateLimiter) {
        this.getHandler = getHandler;
        this.postHandler = postHandler;
        this.deleteHandler = deleteHandler;
        this.rateLimiter = rateLimiter;
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


        String clientIp = req.getClientIp();
        send.setHeader("X-RateLimit-Limit", String.valueOf(rateLimiter.getMaxRequests()));
        send.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimiter.getRemaining(clientIp)));
        send.setHeader("X-RateLimit-Reset", String.valueOf(rateLimiter.getResetSeconds(clientIp)));

        if (!rateLimiter.tryAcquire(clientIp)) {
            send.tooManyRequests(
                    com.app.util.i18n.I18n.get("erro.limite_requisicoes.detalhe", clientIp, rateLimiter.getResetSeconds(clientIp)),
                    rateLimiter.getResetSeconds(clientIp)
            );
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
