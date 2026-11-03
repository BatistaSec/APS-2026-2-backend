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

        String cidade = req.getQueryParam("cidade");
        if (cidade != null && !cidade.isBlank()) {
            int deletados = deletarDao.deletarPorCidade(cidade);
            if (deletados > 0) {
                String msg = (deletados == 1)
                        ? "1 medição do município '" + cidade + "' foi excluída com sucesso."
                        : deletados + " medições do município '" + cidade + "' foram excluídas com sucesso.";
                send.sucesso("{\"mensagem\":\"" + msg + "\"}");
            } else {
                send.naoEncontrado("Nenhuma medição encontrada para o município: " + cidade);
            }
            return;
        }

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

        send.badRequest("Informe o identificador da medição (DELETE /api/v1/medicoes/{id}) ou o município (DELETE /api/v1/medicoes?cidade=Nome).");
    }
}
