package com.ddo.torneios.dto;

public record ResumoAcuraciaDTO(
        Long totalPrevisoes,
        Long acertosResultado,
        Long acertosPlacarExato
) {}