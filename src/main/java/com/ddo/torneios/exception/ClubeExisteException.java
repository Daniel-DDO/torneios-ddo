package com.ddo.torneios.exception;

public class ClubeExisteException extends RuntimeException {
    public ClubeExisteException(String message) {
        super("Já existe um clube com essa sigla ou com esse nome\n"+message+"\n");
    }
}
