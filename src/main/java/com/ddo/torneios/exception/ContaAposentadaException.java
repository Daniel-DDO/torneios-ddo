package com.ddo.torneios.exception;

public class ContaAposentadaException extends RuntimeException {
    public ContaAposentadaException() {
        super("Sua conta está marcada como aposentada. Fale com a administração caso deseje retornar aos torneios.");
    }
}