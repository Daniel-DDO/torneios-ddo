package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;

public record AnularEmMassaRequest(
        @NotBlank String motivo
) {}