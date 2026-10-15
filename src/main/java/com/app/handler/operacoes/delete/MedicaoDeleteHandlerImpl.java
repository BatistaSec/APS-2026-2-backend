package com.app.handler.operacoes.delete;

import com.app.dao.exclusao.DeletarMedicaoDao;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;

import java.io.IOException;


public class MedicaoDeleteHandlerImpl implements MedicaoDeleteHandler {

    private final DeletarMedicaoDao deletarDao;

    public MedicaoDeleteHandlerImpl(DeletarMedicaoDao deletarDao) {
        this.deletarDao = deletarDao;
    }

    @Override
    public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {
        // 1. DELETE /api/medicoes?cidade=Miami
        String cidade = req.getQueryParam("cidade");
        if (cidade != null && !cidade.isBlank()) {
            int deletados = deletarDao.deletarPorCidade(cidade);
            if (deletados > 0) {
                send.sucesso("{\"mensagem\":\"" + deletados + " medições da cidade '" + cidade + "' foram excluídas.\"}");
            } else {
                send.naoEncontrado("Nenhuma medição encontrada para a cidade: " + cidade);
            }
            return;
        }

        // 2. DELETE /api/medicoes/{id}
        Long id = req.getIdFromPath();
        if (id != null) {
            boolean removido = deletarDao.deletarPorId(id);
            if (removido) {
                send.sucesso("{\"mensagem\":\"Medição com ID #" + id + " removida com sucesso.\"}");
            } else {
                send.naoEncontrado("Medição com ID #" + id + " não encontrada.");
            }
            return;
        }

        send.badRequest("Informe o ID (DELETE /api/medicoes/{id}) ou a cidade (DELETE /api/medicoes?cidade=Nome)");
    }
}
