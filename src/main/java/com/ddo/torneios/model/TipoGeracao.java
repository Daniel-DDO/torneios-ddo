package com.ddo.torneios.model;

public enum TipoGeracao {
    SORTEIO_DIRIGIDO, //1º vs 16º, sorteado
    RANKING_PURO,     //1º vs 16º, direto
    SORTEIO_TOTAL,    //aleatório
    POTES_MANUAIS     //manual
}