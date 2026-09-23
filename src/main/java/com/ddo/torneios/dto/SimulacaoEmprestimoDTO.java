package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record SimulacaoEmprestimoDTO(
        BigDecimal valorSolicitado,
        Integer quantidadeParcelas,
        BigDecimal percentualJuros,
        BigDecimal valorTotalComJuros,
        BigDecimal valorParcela,
        BigDecimal totalJuros
) {}