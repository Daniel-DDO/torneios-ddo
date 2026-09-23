package com.ddo.torneios.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EmprestimoRequest(
        @NotNull String jogadorId,
        @NotNull BigDecimal valorSolicitado,
        @NotNull @Min(1) @Max(12) Integer quantidadeParcelas
) {}