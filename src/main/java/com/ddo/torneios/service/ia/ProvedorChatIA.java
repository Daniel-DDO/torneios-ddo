package com.ddo.torneios.service.ia;

public interface ProvedorChatIA {
    String gerarResposta(String prompt) throws Exception;
    String nome();
}