package com.ddo.torneios.dto;

public record LoginResponseDTO(
        String token,
        JogadorDTO jogador
) {
}
