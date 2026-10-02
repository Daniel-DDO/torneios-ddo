package com.ddo.torneios.model;

import java.util.List;

public enum TipoPartida {
    FASE_DE_GRUPOS,
    PONTOS_CORRIDOS,

    MATA_MATA_UNICO,
    MATA_MATA_IDA,
    MATA_MATA_VOLTA,

    DISPUTA_TERCEIRO_LUGAR,
    FINAL_UNICA,
    FINAL_IDA,
    FINAL_VOLTA;

    private static final List<TipoPartida> FINAIS = List.of(FINAL_UNICA, FINAL_IDA, FINAL_VOLTA);

    public static List<TipoPartida> finais() {
        return FINAIS;
    }
}