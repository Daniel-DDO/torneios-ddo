package com.ddo.torneios.dto;

import com.ddo.torneios.model.TipoPartida;

import java.time.LocalDateTime;

public record PartidaFinalDTO(
        String id,
        String faseId,
        TipoPartida tipoPartida,
        LocalDateTime dataHora,
        boolean realizada,
        boolean wo,
        boolean houveProrrogacao,
        String mandanteJogadorId,
        String mandanteJogadorNome,
        String visitanteJogadorId,
        String visitanteJogadorNome,
        Integer golsMandante,
        Integer golsVisitante,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        String linkPartida
) {
}