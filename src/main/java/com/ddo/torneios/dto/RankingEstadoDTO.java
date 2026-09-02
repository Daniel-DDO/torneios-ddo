package com.ddo.torneios.dto;

import com.ddo.torneios.model.RankJogador;

public record RankingEstadoDTO(
        String jogadorId,
        String nome,
        RankJogador rank,
        Integer rankPoints,
        Integer partidasRankeadas,
        Integer strikesRebaixamento
) {}