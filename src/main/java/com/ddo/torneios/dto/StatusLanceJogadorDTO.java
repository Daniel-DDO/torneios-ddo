package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record StatusLanceJogadorDTO(
        Integer prioridade,
        String nomeClube,
        BigDecimal valorOfertado,
        String status
) {}