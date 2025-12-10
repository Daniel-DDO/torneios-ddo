package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JogadorClubeRequest {
    @NotBlank
    private String jogadorId;
    @NotBlank
    private String clubeId;
    @NotBlank
    private String temporadaId;
}
