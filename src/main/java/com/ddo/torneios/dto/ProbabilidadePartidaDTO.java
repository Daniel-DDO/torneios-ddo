package com.ddo.torneios.dto;

public record ProbabilidadePartidaDTO(
        int chanceMandante,
        int chanceEmpate,
        int chanceVisitante,
        String analisePreJogo
) {}