package com.ddo.torneios.dto;

public record ClassificacaoEquipeDTO(
        int posicao, String equipeId, String nome,
        long pontos, long vitorias, long golsPro, long saldoGols,
        long cartoes, Integer melhorPosicao) {}
