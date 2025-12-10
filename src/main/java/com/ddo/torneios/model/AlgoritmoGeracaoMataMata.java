package com.ddo.torneios.model;

public enum AlgoritmoGeracaoMataMata {
    RANKING_PADRAO,     //1º vs 16º, 2º vs 15º (Chave fixa)
    SORTEIO_TOTAL,      //Aleatório puro
    SORTEIO_DIRIGIDO    //Pote A (melhores) x Pote B (piores)
}
