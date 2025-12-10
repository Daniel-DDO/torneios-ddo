package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JogadorClubeRequest {

    @NotBlank(message = "O Jogador é obrigatório")
    private String jogadorId;

    @NotBlank(message = "O Clube é obrigatório")
    private String clubeId;

    @NotBlank(message = "A Temporada é obrigatória")
    private String temporadaId;
}