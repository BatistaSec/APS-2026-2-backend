package com.app.handler.operacoes.post;

import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;
import com.app.model.MedicaoAmbiental;
import com.app.model.MedicaoAmbiental.Origem;
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
        this.registroService   = registroService;
        this.coletorService    = coletorService;
    }

    @Override
    public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {
        String path  = req.getPath();
        String corpo = req.getBody();

        // ── POST /sincronizacoes ──────────────────────────────────────────
        if (path.endsWith("/sincronizacoes") || path.endsWith("/sincronizacoes/")
                || path.endsWith("/sincronizar") || path.endsWith("/sincronizar/")) {

            SincronizacaoRequest syncReq = SincronizacaoRequest.fromJson(corpo);
            if (syncReq.estaVazio()) {
                send.badRequest("Informe a lista de locais: {\"cidades\": [\"Curitiba\"]} ou {\"estados\": [\"PR\"]}");
                return;
            }

            List<MedicaoAmbiental> paraRegistrar = new ArrayList<>();
            for (String local : syncReq.locais()) {
                try {
                    MedicaoRequest reqItem = new MedicaoRequest(null, local, null, null, null, null, null, null, null, null, null);
                    String busca = reqItem.resolverLocalizacaoConsulta();
                    if (busca == null) busca = local;
                    MedicaoAmbiental clima = openWeatherClient.buscarClima(busca);
                    if (clima != null) paraRegistrar.add(clima);
                } catch (Exception e) {
                    System.err.println("Erro na sincronização de '" + local + "': " + e.getMessage());
                }
            }

            List<MedicaoAmbiental> salvas = registroService.registrarLote(paraRegistrar, Origem.SINCRONIZACAO);
            send.criado(MedicaoResponse.fromEntitiesToJson(salvas));
            return;
        }

        // ── POST /coletor/cidades ─────────────────────────────────────────
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
                send.badRequest("Informe a localização: {\"cidade\": \"Curitiba\"} ou {\"estado\": \"PR\"}");
                return;
            }
            coletorService.adicionarCidade(local);
            String msg = "Localização '" + local + "' adicionada à coleta automática com sucesso.";
            String jsonResp = String.format("{\"mensagem\":\"%s\",\"cidades\":%s}", msg,
                    "[" + coletorService.getCidades().stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",")) + "]");
            send.sucesso(jsonResp);
            return;
        }

        // ── POST /coletor/execucoes ───────────────────────────────────────
        if (path.contains("/coletor/execucoes") || path.contains("/coletor/executar")) {
            coletorService.executarCicloColeta();
            send.sucesso("{\"mensagem\":\"Ciclo de coleta meteorológica executado com sucesso.\"}");
            return;
        }

        // ── POST /medicoes com localização (busca OWM) ────────────────────
        MedicaoRequest dto = MedicaoRequest.fromJson(corpo);

        if (!dto.temLocalizacao()) {
            String qCid  = req.getQueryParam("cidade");
            String qEst  = req.getQueryParam("estado");
            String qPais = req.getQueryParam("pais");
            if (qCid != null || qEst != null) {
                dto = new MedicaoRequest(qCid, qEst, qPais, dto.dataHora(), dto.temperatura(), dto.umidade(),
                        dto.precipitacao(), dto.velocidadeVento(), dto.pressaoAtmosferica(), dto.direcaoVento(), dto.indiceUv());
            }
        }

        if (dto.temLocalizacao()) {
            MedicaoAmbiental medicao = openWeatherClient.buscarClima(dto.resolverLocalizacaoConsulta());
            // sobrescreve campos opcionais se o cliente enviou valores
            if (dto.dataHora()           != null) medicao.setDataHora(dto.dataHora());
            if (dto.temperatura()        != null) medicao.setTemperatura(dto.temperatura());
            if (dto.umidade()            != null) medicao.setUmidade(dto.umidade());
            if (dto.precipitacao()       != null) medicao.setPrecipitacao(dto.precipitacao());
            if (dto.velocidadeVento()    != null) medicao.setVelocidadeVento(dto.velocidadeVento());
            if (dto.pressaoAtmosferica() != null) medicao.setPressaoAtmosferica(dto.pressaoAtmosferica());
            if (dto.direcaoVento()       != null) medicao.setDirecaoVento(dto.direcaoVento());
            if (dto.indiceUv()           != null) medicao.setIndiceUv(dto.indiceUv());
            // propaga estado/país do DTO para enriquecer a resolução de Cidade
            if (dto.estado() != null && !dto.estado().isBlank()) medicao.setEstadoTemp(dto.estado());
            if (dto.pais()   != null && !dto.pais().isBlank())   medicao.setPaisTemp(dto.pais());

            MedicaoAmbiental salva = registroService.registrar(medicao, Origem.POST);
            send.criado(MedicaoResponse.fromEntity(salva).toJson());
            return;
        }

        // ── POST /medicoes manual (sem cidade) ────────────────────────────
        if (dto.temperatura() == null || dto.umidade() == null) {
            send.badRequest("Informe a localização (ex: {\"cidade\": \"São Paulo\"}) ou forneça ao menos temperatura e umidade para registro manual.");
            return;
        }

        MedicaoAmbiental manual = new MedicaoAmbiental();
        manual.setEstacaoIdTemp("ESTACAO-MANUAL");
        manual.setDataHora(dto.dataHora() != null ? dto.dataHora() : LocalDateTime.now());
        manual.setTemperatura(dto.temperatura());
        manual.setUmidade(dto.umidade());
        manual.setPrecipitacao(dto.precipitacao() != null ? dto.precipitacao() : false);
        manual.setVelocidadeVento(dto.velocidadeVento() != null ? dto.velocidadeVento() : 0.0);
        manual.setPressaoAtmosferica(dto.pressaoAtmosferica());
        manual.setDirecaoVento(dto.direcaoVento());
        manual.setIndiceUv(dto.indiceUv());

        MedicaoAmbiental salva = registroService.registrar(manual, Origem.MANUAL);
        send.criado(MedicaoResponse.fromEntity(salva).toJson());
    }
}
