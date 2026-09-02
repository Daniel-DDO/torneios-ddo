package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;

public record TrocarJogadorClubePartidaRequest(
        @NotBlank String partidaId,
        @NotBlank String jogadorClubeAntigoId,
        @NotBlank String novoJogadorId
) {}