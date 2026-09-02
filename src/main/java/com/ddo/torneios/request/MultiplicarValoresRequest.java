package com.ddo.torneios.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MultiplicarValoresRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal multiplicador
) {}