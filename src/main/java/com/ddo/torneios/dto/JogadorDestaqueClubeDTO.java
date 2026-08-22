package com.ddo.torneios.dto;

public record JogadorDestaqueClubeDTO(
        String jogadorId,
        String jogadorNome,
        String jogadorImagem,
        Long totalConquistas
) {
}