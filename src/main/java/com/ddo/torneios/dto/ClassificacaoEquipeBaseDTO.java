package com.ddo.torneios.dto;

public record ClassificacaoEquipeBaseDTO(
        String equipeId, String nome,
        Long pontos, Long vitorias, Long golsPro, Long saldoGols, Integer melhorPosicao) {}

