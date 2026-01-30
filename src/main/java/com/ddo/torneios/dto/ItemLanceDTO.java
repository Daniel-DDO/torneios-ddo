package com.ddo.torneios.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemLanceDTO(
        @NotNull String clubeId,
        @NotNull BigDecimal valor,
        @NotNull @Min(1) @Max(5) Integer prioridade
) {}
