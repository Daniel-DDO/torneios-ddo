package com.ddo.torneios.exception;

public class CompeticaoExisteException extends RuntimeException {
    public CompeticaoExisteException(String message) {
        super("Já existe uma competição com esse nome: "+message);
    }
}
