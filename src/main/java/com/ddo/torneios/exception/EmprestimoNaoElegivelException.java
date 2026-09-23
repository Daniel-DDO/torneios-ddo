package com.ddo.torneios.exception;

public class EmprestimoNaoElegivelException extends RuntimeException {
    public EmprestimoNaoElegivelException(String motivo) {
        super(motivo);
    }
}