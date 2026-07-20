package com.ddo.torneios.dto;

public record RecordeTemporadaDTO(
        String jogadorId, String jogadorNome, String jogadorImagem,
        String temporadaNome, int valor, int partidasJogadas
) {}
