package com.ddo.torneios.dto;

public record TituloCampeaoDTO(
        String jogadorId,
        String jogadorNome,
        String jogadorImagem,
        Long quantidadeTitulos
) {
}