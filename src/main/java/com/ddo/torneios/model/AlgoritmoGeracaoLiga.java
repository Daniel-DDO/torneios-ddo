package com.ddo.torneios.model;

public enum AlgoritmoGeracaoLiga {
    TODOS_CONTRA_TODOS_IDA_VOLTA,   //ex: LaLiga
    TODOS_CONTRA_TODOS_UNICO,       //ex: metade de uma LaLiga
    FASE_GRUPOS,                    //ex: fase de grupos
    SISTEMA_SUICO,                  //ex: xadrez/e-sports
    ALEATORIO_BALANCEADO            //ex: fase de liga UCL
}
