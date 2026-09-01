package com.ddo.torneios.dto;

import java.util.List;

public record ProbabilidadePartidaDTO(
        int chanceMandante,
        int chanceEmpate,
        int chanceVisitante,
        String analisePreJogo,
        PlacarCotadoDTO placarCotado
) {
    public record PlacarCotadoDTO(
            int golsMandante,
            int golsVisitante,
            double probabilidadeDessePlacar,
            double expectativaGolsMandante,
            double expectativaGolsVisitante,
            double probabilidadeAmbosMarcam,
            double probabilidadeMaisDe2Meio,
            List<PlacarProvavelDTO> top3PlacaresMaisProvaveis,
            String observacao
    ) {}

    public record PlacarProvavelDTO(
            int golsMandante,
            int golsVisitante,
            double probabilidade
    ) {}
}