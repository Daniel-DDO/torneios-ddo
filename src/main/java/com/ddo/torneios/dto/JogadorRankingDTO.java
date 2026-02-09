package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record JogadorRankingDTO(
        String id,
        String nome,
        String discord,
        String imagem,
        String cargo,
        BigDecimal saldo
) {}