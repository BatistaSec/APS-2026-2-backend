package com.app.handler.operacoes.post;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.request.MedicaoRequest;
import com.app.model.dto.request.SincronizacaoRequest;
import com.app.model.dto.response.MedicaoResponse;
import com.app.service.coleta.ColetorAgendadoService;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.registro.RegistroMedicaoService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MedicaoPostHandlerImpl implements MedicaoPostHandler {

    private final OpenWeatherClient openWeatherClient;
    private final RegistroMedicaoService registroService;
    private final ColetorAgendadoService coletorService;

    public MedicaoPostHandlerImpl(OpenWeatherClient openWeatherClient,
                                  RegistroMedicaoService registroService,
                                  ColetorAgendadoService coletorService) {
        this.openWeatherClient = openWeatherClient;
        this.registroService = registroService;
        this.coletorService = coletorService;
    }

    @Override
    public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {

        String path = req.getPath();
        String corpo = req.getBody();


        if (path.endsWith("/sincronizacoes") || path.endsWith("/sincronizacoes/")
                || path.endsWith("/sincronizar") || path.endsWith("/sincronizar/")) {
            SincronizacaoRequest syncReq = SincronizacaoRequest.fromJson(corpo);
            if (syncReq.estaVazio()) {
                send.badRequest("Informe a lista de locais no formato: {\"cidades\": [\"Curitiba\", \"Tokyo\"]} ou {\"estados\": [\"PR\", \"SP\"]}");
                return;
            }

            List<MedicaoAmbiental> paraRegistrar = new ArrayList<>();
            for (String local : syncReq.locais()) {
                try {
                    MedicaoRequest reqItem = new MedicaoRequest(null, local, null, null, null, null, null, null, null, null, null);
                    String busca = reqItem.resolverLocalizacaoConsulta();
                    if (busca == null) busca = local;

                    MedicaoAmbiental clima = openWeatherClient.buscarClima(busca);
                    if (clima != null) {
                        paraRegistrar.add(clima);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao obter clima para sincronização do local " + local + ": " + e.getMessage());
                }
            }

            List<MedicaoAmbiental> salvas = registroService.registrarLote(paraRegistrar);
            send.criado(MedicaoResponse.fromEntitiesToJson(salvas));
            return;
        }


        if (path.contains("/coletor/cidades")) {
            MedicaoRequest cidReq = MedicaoRequest.fromJson(corpo);
            String local = cidReq.resolverLocalizacaoConsulta();
            if (local == null || local.isBlank()) {
                String paramCid = req.getQueryParam("cidade");
                String paramEst = req.getQueryParam("estado");
                local = new MedicaoRequest(paramCid, paramEst, null, null, null, null, null, null, null, null, null)
                        .resolverLocalizacaoConsulta();
            }
            if (local == null || local.isBlank()) {
                send.badRequest("Informe a localização desejada: {\"cidade\": \"Curitiba\"} ou {\"estado\": \"PR\"}");
                return;
            }

            coletorService.adicionarCidade(local);
            String msg = com.app.util.i18n.I18n.get("sucesso.coleta.adicionada", local);
            String jsonResposta = String.format("{\"mensagem\":\"%s\",\"cidades\":%s}",
                    msg,
                    "[" + coletorService.getCidades().stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",")) + "]");
            send.sucesso(jsonResposta);
            return;
        }


        if (path.contains("/coletor/execucoes") || path.contains("/coletor/executar")) {
            coletorService.executarCicloColeta();
            send.sucesso("{\"mensagem\":\"" + com.app.util.i18n.I18n.get("sucesso.coleta.executada") + "\"}");
            return;
        }


        MedicaoRequest dto = MedicaoRequest.fromJson(corpo);


        if (!dto.temLocalizacao()) {
            String qCid = req.getQueryParam("cidade");
            String qEst = req.getQueryParam("estado");
            String qPais = req.getQueryParam("pais");
            if (qCid != null || qEst != null) {
                dto = new MedicaoRequest(qCid, qEst, qPais, dto.dataHora(), dto.temperatura(), dto.umidade(),
                        dto.precipitacao(), dto.velocidadeVento(), dto.pressaoAtmosferica(), dto.direcaoVento(), dto.indiceUv());
            }
        }


        if (dto.temLocalizacao()) {
            String termoBusca = dto.resolverLocalizacaoConsulta();
            MedicaoAmbiental medicao = openWeatherClient.buscarClima(termoBusca);


            if (dto.dataHora() != null) medicao.setDataHora(dto.dataHora());
            if (dto.temperatura() != null) medicao.setTemperatura(dto.temperatura());
            if (dto.umidade() != null) medicao.setUmidade(dto.umidade());
            if (dto.precipitacao() != null) medicao.setPrecipitacao(dto.precipitacao());
            if (dto.velocidadeVento() != null) medicao.setVelocidadeVento(dto.velocidadeVento());
            if (dto.pressaoAtmosferica() != null) medicao.setPressaoAtmosferica(dto.pressaoAtmosferica());
            if (dto.direcaoVento() != null) medicao.setDirecaoVento(dto.direcaoVento());
            if (dto.indiceUv() != null) medicao.setIndiceUv(dto.indiceUv());


            MedicaoAmbiental salva = registroService.registrar(medicao);
            send.criado(MedicaoResponse.fromEntity(salva).toJson());
            return;
        }


        if (dto.temperatura() == null || dto.umidade() == null) {
            send.badRequest("Informe a localização da medição (ex: {\"cidade\": \"Curitiba\"}) ou forneça ao menos temperatura e umidade para registro manual.");
            return;
        }

        MedicaoAmbiental manual = new MedicaoAmbiental();
        // estacaoId é NOT NULL — usa valor padrão quando não informado
        manual.setEstacaoId("ESTACAO-MANUAL");
        manual.setDataHora(dto.dataHora() != null ? dto.dataHora() : LocalDateTime.now());
        manual.setTemperatura(dto.temperatura());
        manual.setUmidade(dto.umidade());
        // precipitacao é NOT NULL — usa false como padrão quando não informado
        manual.setPrecipitacao(dto.precipitacao() != null ? dto.precipitacao() : Boolean.FALSE);
        // velocidadeVento é NOT NULL — usa 0.0 como padrão quando não informado
        manual.setVelocidadeVento(dto.velocidadeVento() != null ? dto.velocidadeVento() : 0.0);
        manual.setPressaoAtmosferica(dto.pressaoAtmosferica());
        manual.setDirecaoVento(dto.direcaoVento());
        manual.setIndiceUv(dto.indiceUv());

        MedicaoAmbiental salva = registroService.registrar(manual);
        send.criado(MedicaoResponse.fromEntity(salva).toJson());
    }
}
