package com.ddo.torneios.dto;

public record RecordeJogadorDTO(
        String jogadorId, String jogadorNome, String jogadorImagem,
        String valorFormatado, int valorBruto
) {}
