package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record ObservacaoDisponibilidadeDTO(
        Long id,
        String texto,
        LocalDateTime criadoEm
) {}
