package com.ddo.torneios.exception;

public class JogadorExisteException extends RuntimeException {
    public JogadorExisteException(String message) {
        super("Já existe um jogador cadastrado com esse discord: "+message);
    }
}
