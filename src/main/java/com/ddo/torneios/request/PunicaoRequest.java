package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PunicaoRequest {
    @NotNull(message = "O ID da participação na fase é obrigatório")
    private String participacaoFaseId;

    @NotNull(message = "A quantidade de pontos é obrigatória")
    private Integer pontos;

    @NotBlank(message = "O motivo é obrigatório")
    private String motivo;
}