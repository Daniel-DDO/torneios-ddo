package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record ResultadoLeilaoDTO(
        String nomeClube,
        String imagemClube,
        String nomeJogador,
        BigDecimal valorPago
) {}