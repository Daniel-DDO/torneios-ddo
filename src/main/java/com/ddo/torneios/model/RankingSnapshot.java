package com.ddo.torneios.model;

public record RankingSnapshot(
        int pontosAntes, RankJogador rankAntes, int partidasAntes, int strikesAntes,
        int pontosDepois, RankJogador rankDepois, int partidasDepois, int strikesDepois
) {}