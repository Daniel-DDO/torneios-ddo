package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record JogadorClubeInscritoDTO(
        String id,
        String jogadorNome,
        String clubeNome,
        String clubeImagem,
        Integer jogos,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsMarcados,
        Integer golsSofridos,
        BigDecimal pontosCoeficiente
) {}