package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TorneioRequest {
    @NotBlank(message = "O nome do torneio é obrigatório")
    private String nome;

    @NotBlank(message = "A temporada é obrigatória")
    private String temporadaId;

    @NotBlank(message = "A competição é obrigatória")
    private String competicaoId;
}