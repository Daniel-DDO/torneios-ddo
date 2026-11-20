package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;

public record CriarEquipeRequest(@NotBlank String nome, String imagem,
                                 @NotBlank String torneioId, @NotBlank String capitaoId) {}
