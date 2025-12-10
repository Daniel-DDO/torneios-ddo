package com.ddo.torneios.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String message) {
        super("Já existe um jogador cadastrado com esse email: "+message+"\n");
    }
}
