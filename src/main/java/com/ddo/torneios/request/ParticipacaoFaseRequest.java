package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParticipacaoFaseRequest {

    @NotBlank(message = "A Fase é obrigatória")
    private String faseId;

    @NotBlank(message = "O Jogador/Clube (Inscrição) é obrigatório")
    private String jogadorClubeId;
}