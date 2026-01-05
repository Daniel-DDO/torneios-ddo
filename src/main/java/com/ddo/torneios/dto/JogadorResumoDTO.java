package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record JogadorResumoDTO(
        String id,
        String nome,
        String discord,
        BigDecimal pontosCoeficiente
) {
}
