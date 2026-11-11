package com.ddo.torneios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarAdvertenciaRequest(
        @NotBlank @Size(max = 4000) String motivo
) {
}
