package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record JogadorComparacaoBaseDTO(
        String id,
        String nome,
        String discord,
        String imagem,
        Integer titulos,
        Integer finais,
        Integer partidasJogadas,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsMarcados,
        Integer golsSofridos,
        BigDecimal saldoVirtual,
        BigDecimal pontosCoeficiente
) {}