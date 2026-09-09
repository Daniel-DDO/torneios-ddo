package com.ddo.torneios.dto;

public record AcuraciaModeloDTO(
        long totalPrevisoes,
        long acertosResultado,
        long acertosPlacarExato,
        double percentualAcertoResultado,
        double percentualAcertoPlacarExato
) {}