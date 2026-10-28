package com.app.handler.operacoes.get;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.request.MedicaoRequest;
import com.app.model.dto.response.EstatisticasClimaticas;
import com.app.model.dto.response.MedicaoResponse;
import com.app.model.dto.response.PaginatedResponse;
import com.app.service.estatistica.EstatisticaService;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.registro.RegistroMedicaoService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MedicaoGetHandlerImpl implements MedicaoGetHandler {

    private final ConsultaMedicaoDao consultaDao;
    private final OpenWeatherClient openWeatherClient;
    private final RegistroMedicaoService registroService;
    private final EstatisticaService estatisticaService;

    public MedicaoGetHandlerImpl(ConsultaMedicaoDao consultaDao,
                                 OpenWeatherClient openWeatherClient,
                                 RegistroMedicaoService registroService,
                                 EstatisticaService estatisticaService) {
        this.consultaDao = consultaDao;
        this.openWeatherClient = openWeatherClient;
        this.registroService = registroService;
        this.estatisticaService = estatisticaService;
    }

    @Override
    public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {
        String path = req.getPath();


        if (path.contains("/cidades/") && path.endsWith("/medicoes")) {
            String cidadeExtraida = extrairParametroEntre(path, "/cidades/", "/medicoes");
            tratarListagem(req, send, cidadeExtraida);
            return;
        }

        if (path.contains("/cidades/") && path.endsWith("/estatisticas")) {
            String cidadeExtraida = extrairParametroEntre(path, "/cidades/", "/estatisticas");
            int limite = req.getQueryParamInt("limite", 50);
            EstatisticasClimaticas stats = estatisticaService.calcularEstatisticasPorCidade(cidadeExtraida, limite);
            send.sucesso(stats.toJson());
            return;
        }


        if (path.contains("/estacoes/") && path.endsWith("/medicoes")) {
            String estacaoId = extrairParametroEntre(path, "/estacoes/", "/medicoes");
            int limite = req.getQueryParamInt("limite", 50);
            List<MedicaoAmbiental> medicoes = consultaDao.listarPorEstacao(estacaoId, limite);
            send.sucesso(MedicaoResponse.fromEntitiesToJson(medicoes));
            return;
        }


        if (path.endsWith("/cidades") || path.endsWith("/cidades/")) {
            List<String> cidades = consultaDao.listarCidades();
            String json = "[" + cidades.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",")) + "]";
            send.sucesso(json);
            return;
        }


        String cidade = req.getQueryParam("cidade");
        String estado = req.getQueryParam("estado");
        String pais = req.getQueryParam("pais");
        MedicaoRequest locReq = new MedicaoRequest(cidade, estado, pais, null, null, null, null, null, null, null, null);


        if (path.endsWith("/atual") || path.endsWith("/atual/")) {
            if (locReq.temLocalizacao()) {
                String termoBusca = locReq.resolverLocalizacaoConsulta();
                try {

                    MedicaoAmbiental viva = openWeatherClient.buscarClima(termoBusca);
                    MedicaoAmbiental salva = registroService.registrar(viva);
                    send.sucesso(MedicaoResponse.fromEntity(salva).toJson());
                    return;
                } catch (Exception ignored) {

                }
            }

            String filtroBanco = (cidade != null && !cidade.isBlank()) ? cidade : estado;
            Optional<MedicaoAmbiental> ultima = consultaDao.buscarUltimaPorCidade(filtroBanco);
            if (ultima.isPresent()) {
                send.sucesso(MedicaoResponse.fromEntity(ultima.get()).toJson());
            } else {
                send.naoEncontrado(com.app.util.i18n.I18n.get("listagem.nenhuma_medicao"));
            }
            return;
        }


        if (path.endsWith("/estatisticas") || path.endsWith("/estatisticas/")) {
            int limite = req.getQueryParamInt("limite", 50);
            String filtroLocal = (cidade != null && !cidade.isBlank()) ? cidade : estado;
            EstatisticasClimaticas stats = (filtroLocal != null && !filtroLocal.isBlank())
                    ? estatisticaService.calcularEstatisticasPorCidade(filtroLocal, limite)
                    : estatisticaService.calcularEstatisticasRecentes(limite);
            send.sucesso(stats.toJson());
            return;
        }


        Long id = req.getIdFromPath();
        if (id != null) {
            Optional<MedicaoAmbiental> busca = consultaDao.buscarPorId(id);
            if (busca.isPresent()) {
                send.sucesso(MedicaoResponse.fromEntity(busca.get()).toJson());
            } else {
                send.naoEncontrado(com.app.util.i18n.I18n.get("listagem.medicao_id_nao_encontrada", id));
            }
            return;
        }


        tratarListagem(req, send, cidade != null ? cidade : estado);
    }

    private void tratarListagem(HttpRequest req, HttpResponseSender send, String cidadeFiltro) throws IOException {
        String path = req.getPath();
        boolean isV1 = path.contains("/v1/");
        boolean temParamPagina = req.getQueryParam("page") != null || req.getQueryParam("pageSize") != null;


        Double tempMin = req.getQueryParamDouble("tempMin", null);
        Double tempMax = req.getQueryParamDouble("tempMax", null);
        Boolean precipitacao = req.getQueryParam("precipitacao") != null
                ? req.getQueryParamBoolean("precipitacao", null)
                : null;
        String sort = req.getQueryParam("sort");
        String order = req.getQueryParam("order");


        if (isV1 || temParamPagina) {
            int page = req.getQueryParamInt("page", 1);
            int pageSize = req.getQueryParamInt("pageSize", 20);

            List<MedicaoAmbiental> lista = consultaDao.listarComFiltros(
                    cidadeFiltro, tempMin, tempMax, precipitacao, sort, order, page, pageSize
            );
            long total = consultaDao.contarComFiltros(cidadeFiltro, tempMin, tempMax, precipitacao);

            List<MedicaoResponse> dtos = lista.stream()
                    .map(MedicaoResponse::fromEntity)
                    .toList();

            PaginatedResponse paginada = PaginatedResponse.of(dtos, page, pageSize, total);
            send.sucesso(paginada.toJson());
            return;
        }


        int limite = req.getQueryParamInt("limite", 30);
        List<MedicaoAmbiental> lista = (cidadeFiltro != null && !cidadeFiltro.isBlank())
                ? consultaDao.listarPorCidade(cidadeFiltro, limite)
                : consultaDao.listarUltimas(limite);

        send.sucesso(MedicaoResponse.fromEntitiesToJson(lista));
    }

    private String extrairParametroEntre(String path, String inicio, String fim) {
        try {
            int startIdx = path.indexOf(inicio) + inicio.length();
            int endIdx = fim != null ? path.indexOf(fim, startIdx) : path.length();
            if (endIdx < 0) endIdx = path.length();
            String raw = path.substring(startIdx, endIdx);
            return URLDecoder.decode(raw, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }
}
