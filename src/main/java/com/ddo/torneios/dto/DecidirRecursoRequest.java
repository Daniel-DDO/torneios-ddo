package com.ddo.torneios.dto;

import com.ddo.torneios.model.ResultadoRecurso;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecidirRecursoRequest(
        @NotNull ResultadoRecurso resultado,
        @Min(1) @Max(20) Integer novoMultiplicador,
        @NotBlank @Size(max = 4000) String fundamentacao
) {
}
