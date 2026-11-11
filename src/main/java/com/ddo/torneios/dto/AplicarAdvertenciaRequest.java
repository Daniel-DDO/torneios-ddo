package com.ddo.torneios.dto;

import com.ddo.torneios.model.NivelAdvertencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AplicarAdvertenciaRequest(
        @NotNull NivelAdvertencia nivel,
        @NotBlank @Size(max = 4000) String fato,
        @NotBlank @Size(max = 100) String artigo,
        @Min(1) @Max(20) Integer multiplicador,
        @Size(max = 4000) String fundamentacao,
        @Min(1) @Max(3) Integer partidasSuspensao,
        @Min(10) @Max(90) Integer diasSuspensao
) {
}
