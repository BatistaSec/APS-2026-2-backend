package com.app.handler.operacoes.get;

import com.app.dao.consulta.ConsultaMedicaoDao;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;
import com.app.model.MedicaoAmbiental;
import com.app.model.dto.request.MedicaoRequest;
import com.app.model.dto.response.EstatisticasClimaticas;
import com.app.model.dto.response.MedicaoResponse;
import com.app.service.estatistica.EstatisticaService;
import com.app.service.openweather.OpenWeatherClient;
import com.app.service.registro.RegistroMedicaoService;

import java.io.IOException;
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

        // 1. GET /api/medicoes/cidades
        if (path.endsWith("/cidades") || path.endsWith("/cidades/")) {
            List<String> cidades = consultaDao.listarCidades();
            String json = "[" + cidades.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(",")) + "]";
            send.sucesso(json);
            return;
        }

        // Extrai parâmetros comuns de localização (suporta cidade, estado e país)
        String cidade = req.getQueryParam("cidade");
        String estado = req.getQueryParam("estado");
        String pais = req.getQueryParam("pais");
        MedicaoRequest locReq = new MedicaoRequest(cidade, estado, pais, null, null, null, null, null, null, null, null);

        // 2. GET /api/medicoes/atual?cidade=Tokyo ou ?estado=PR
        if (path.endsWith("/atual") || path.endsWith("/atual/")) {
            if (locReq.temLocalizacao()) {
                String termoBusca = locReq.resolverLocalizacaoConsulta();
                try {
                    // Consulta ao vivo no OpenWeatherMap e sincroniza no MySQL
                    MedicaoAmbiental viva = openWeatherClient.buscarClima(termoBusca);
                    MedicaoAmbiental salva = registroService.registrar(viva);
                    send.sucesso(MedicaoResponse.fromEntity(salva).toJson());
                    return;
                } catch (Exception ignored) {
                    // Fallback para histórico do banco caso a API externa falhe
                }
            }

            String filtroBanco = (cidade != null && !cidade.isBlank()) ? cidade : estado;
            Optional<MedicaoAmbiental> ultima = consultaDao.buscarUltimaPorCidade(filtroBanco);
            if (ultima.isPresent()) {
                send.sucesso(MedicaoResponse.fromEntity(ultima.get()).toJson());
            } else {
                send.naoEncontrado("Nenhuma medição encontrada.");
            }
            return;
        }

        // 3. GET /api/medicoes/estatisticas?cidade=Curitiba ou ?estado=PR
        if (path.endsWith("/estatisticas") || path.endsWith("/estatisticas/")) {
            int limite = req.getQueryParamInt("limite", 50);
            String filtroLocal = (cidade != null && !cidade.isBlank()) ? cidade : estado;
            EstatisticasClimaticas stats = (filtroLocal != null && !filtroLocal.isBlank())
                    ? estatisticaService.calcularEstatisticasPorCidade(filtroLocal, limite)
                    : estatisticaService.calcularEstatisticasRecentes(limite);
            send.sucesso(stats.toJson());
            return;
        }

        // 4. GET /api/medicoes/{id}
        Long id = req.getIdFromPath();
        if (id != null) {
            Optional<MedicaoAmbiental> busca = consultaDao.buscarPorId(id);
            if (busca.isPresent()) {
                send.sucesso(MedicaoResponse.fromEntity(busca.get()).toJson());
            } else {
                send.naoEncontrado("Medição com ID #" + id + " não encontrada.");
            }
            return;
        }

        // 5. GET /api/medicoes?cidade=Curitiba ou ?estado=PR&limite=30
        int limite = req.getQueryParamInt("limite", 30);
        String filtroLocal = (cidade != null && !cidade.isBlank()) ? cidade : estado;
        List<MedicaoAmbiental> lista = (filtroLocal != null && !filtroLocal.isBlank())
                ? consultaDao.listarPorCidade(filtroLocal, limite)
                : consultaDao.listarUltimas(limite);

        send.sucesso(MedicaoResponse.fromEntitiesToJson(lista));
    }
}
