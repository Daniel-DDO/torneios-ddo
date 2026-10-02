package com.ddo.torneios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObservacaoRequestDTO(
        @NotBlank @Size(max = 500) String texto
) {}
