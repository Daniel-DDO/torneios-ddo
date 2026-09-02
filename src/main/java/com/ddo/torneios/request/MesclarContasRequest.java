package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MesclarContasRequest(
        @NotBlank String idPrincipal,
        @NotEmpty List<String> idsAntigos
) {}