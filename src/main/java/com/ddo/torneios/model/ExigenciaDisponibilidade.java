package com.ddo.torneios.model;

import lombok.Getter;

/** Mínimo de dias da semana (com pelo menos 1 turno válido) exigido por tipo de campeonato. */
@Getter
public enum ExigenciaDisponibilidade {
    /** Liga + mata-mata */
    CAMPEONATO_LONGO(4),
    /** Só mata-mata ou até 8 jogos */
    CAMPEONATO_CURTO(2);

    private final int minDias;

    ExigenciaDisponibilidade(int minDias) {
        this.minDias = minDias;
    }
}
