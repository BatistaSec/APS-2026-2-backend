package com.app.dao.exclusao;


public interface DeletarMedicaoDao {


    boolean deletarPorId(Long id);


    int deletarPorCidade(String cidade);
}
