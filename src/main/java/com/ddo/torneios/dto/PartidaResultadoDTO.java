package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record PartidaResultadoDTO(
        String partidaId,
        String mandanteJogadorId,
        String visitanteJogadorId,
        Integer golsMandante,
        Integer golsVisitante,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        boolean wo,
        LocalDateTime dataHora
) {}