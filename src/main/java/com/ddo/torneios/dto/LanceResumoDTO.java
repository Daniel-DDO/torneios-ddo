package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record LanceResumoDTO(
        String clubeId,
        BigDecimal valorAtual,
        String nomeJogadorGanhando,
        String jogadorId
) {}