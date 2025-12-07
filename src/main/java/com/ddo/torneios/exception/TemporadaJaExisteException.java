package com.ddo.torneios.exception;

public class TemporadaJaExisteException extends RuntimeException {
    public TemporadaJaExisteException(String message) {
        super("Já existe uma temporada com esse nome: "+message);
    }
}
