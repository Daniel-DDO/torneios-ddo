package com.ddo.torneios.dto;

public record AmistosoLadoDTO(
        String jogadorId,
        String jogadorNome,
        String jogadorImagem,
        String clubeId,
        String clubeNome,
        String clubeImagem,
        String clubeSigla
) {}