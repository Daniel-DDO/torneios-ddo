package com.ddo.torneios.dto;

import jakarta.validation.constraints.Min;

public record RegistrarResultadoAmistosoRequest(
        @Min(0) Integer golsMandante,
        @Min(0) Integer golsVisitante,
        boolean wo,
        @Min(0) Integer cartoesAmarelosMandante,
        @Min(0) Integer cartoesVermelhosMandante,
        @Min(0) Integer cartoesAmarelosVisitante,
        @Min(0) Integer cartoesVermelhosVisitante,
        String linkPartida
) {}