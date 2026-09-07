package com.ddo.torneios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class JogadorNaoEncontradoException extends ResponseStatusException {
    public JogadorNaoEncontradoException(String id) {
        super(HttpStatus.NOT_FOUND, "Jogador não encontrado: " + id);
    }
}