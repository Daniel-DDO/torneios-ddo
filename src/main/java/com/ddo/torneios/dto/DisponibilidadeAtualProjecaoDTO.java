package com.ddo.torneios.dto;

import java.time.LocalDateTime;

/** Projeção leve usada pelas queries (JPQL SELECT new). */
public record DisponibilidadeAtualProjecaoDTO(
        String jogadorId,
        Integer mascara,
        LocalDateTime criadoEm
) {}
