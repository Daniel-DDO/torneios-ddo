package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TituloRequest(
        @NotBlank String nome,
        @NotNull Integer valor,
        String descricao,
        @NotBlank String imagem,
        String imagemGerarPost
) {}