package com.ddo.torneios.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CriarAmistosoRequest(
        @NotBlank String jogadorMandanteId,
        @NotBlank String clubeMandanteId,
        @NotBlank String jogadorVisitanteId,
        @NotBlank String clubeVisitanteId,
        LocalDateTime dataHora
) {}
