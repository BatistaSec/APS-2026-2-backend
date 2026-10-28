package com.app.handler.operacoes.delete;

import com.app.dao.exclusao.DeletarMedicaoDao;
import com.app.handler.http.request.HttpRequest;
import com.app.handler.http.response.HttpResponseSender;
import com.app.handler.security.SecurityService;

import java.io.IOException;

public class MedicaoDeleteHandlerImpl implements MedicaoDeleteHandler {

    private final DeletarMedicaoDao deletarDao;
    private final SecurityService securityService;

    public MedicaoDeleteHandlerImpl(DeletarMedicaoDao deletarDao) {
        this(deletarDao, new SecurityService());
    }

    public MedicaoDeleteHandlerImpl(DeletarMedicaoDao deletarDao, SecurityService securityService) {
        this.deletarDao = deletarDao;
        this.securityService = securityService;
    }

    @Override
    public void tratar(HttpRequest req, HttpResponseSender send) throws IOException {

        if (!securityService.autorizarOperacaoEscrita(req)) {
            send.unauthorized("Acesso não autorizado: token de autenticação (Bearer) ausente ou inválido para operação de exclusão.");
            return;
        }


        String cidade = req.getQueryParam("cidade");
        if (cidade != null && !cidade.isBlank()) {
            int deletados = deletarDao.deletarPorCidade(cidade);
            if (deletados > 0) {
                String msg = (deletados == 1)
                        ? com.app.util.i18n.I18n.get("sucesso.medicao.excluida_cidade_singular", cidade)
                        : com.app.util.i18n.I18n.get("sucesso.medicoes.excluidas_cidade", deletados, cidade);
                send.sucesso("{\"mensagem\":\"" + msg + "\"}");
            } else {
                send.naoEncontrado(com.app.util.i18n.I18n.get("listagem.cidade_nao_encontrada", cidade));
            }
            return;
        }


        Long id = req.getIdFromPath();
        if (id != null) {
            boolean removido = deletarDao.deletarPorId(id);
            if (removido) {
                send.sucesso("{\"mensagem\":\"" + com.app.util.i18n.I18n.get("sucesso.medicao.excluida_id", id) + "\"}");
            } else {
                send.naoEncontrado(com.app.util.i18n.I18n.get("listagem.medicao_id_nao_encontrada", id));
            }
            return;
        }

        send.badRequest("Informe o identificador da medição (DELETE /api/v1/medicoes/{id}) ou o município (DELETE /api/v1/medicoes?cidade=Nome).");
    }
}
