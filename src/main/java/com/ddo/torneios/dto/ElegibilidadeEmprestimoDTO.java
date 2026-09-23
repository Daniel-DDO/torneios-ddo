package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record ElegibilidadeEmprestimoDTO(
        boolean elegivel,
        String motivo,
        BigDecimal limiteMaximo,
        BigDecimal saldoMinimoExigido,
        BigDecimal saldoAtual,
        int partidasJogadas
) {}