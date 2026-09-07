package com.ddo.torneios.exception;

public class ContaBloqueadaException extends RuntimeException {
    public ContaBloqueadaException(String mensagem) {
        super(mensagem);
    }
}