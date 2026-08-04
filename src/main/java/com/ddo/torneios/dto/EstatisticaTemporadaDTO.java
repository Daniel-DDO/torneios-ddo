package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record EstatisticaTemporadaDTO(
        String jogadorClubeId,
        String jogadorId,
        String jogadorNome,
        Integer golsMarcados,
        Integer golsSofridos,
        Integer partidasJogadas,
        Integer cartoesAmarelos,
        Integer cartoesVermelhos,
        BigDecimal pontosCoeficiente,
        Integer rankPoints
) {}