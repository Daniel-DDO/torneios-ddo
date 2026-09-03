package com.ddo.torneios.dto;

import com.ddo.torneios.model.Cargo;

import java.math.BigDecimal;

public record JogadorRankingDTO(
        String id,
        String nome,
        String discord,
        String imagem,
        Cargo cargo,
        BigDecimal saldo
) {}