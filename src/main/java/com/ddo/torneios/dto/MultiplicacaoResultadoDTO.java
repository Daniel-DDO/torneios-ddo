package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record MultiplicacaoResultadoDTO(
        int clubesAtualizados,
        BigDecimal multiplicador
) {}