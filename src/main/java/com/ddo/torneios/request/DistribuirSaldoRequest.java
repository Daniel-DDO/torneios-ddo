package com.ddo.torneios.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DistribuirSaldoRequest(
        @NotNull @Positive BigDecimal valor,
        String motivo
) {}