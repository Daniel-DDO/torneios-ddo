package com.ddo.torneios.dto;

import com.ddo.torneios.model.TipoPartida;

import java.time.LocalDateTime;

public record PartidaConfrontoDTO(
        String id,
        TipoPartida tipoPartida,
        boolean realizada,
        LocalDateTime dataHora,
        String estadio,
        JogadorClubeResumoDTO mandante,
        JogadorClubeResumoDTO visitante,
        Integer golsMandante,
        Integer golsVisitante,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        boolean houvePenaltis
) {}