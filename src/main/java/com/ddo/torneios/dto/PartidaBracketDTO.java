package com.ddo.torneios.dto;

public record PartidaBracketDTO(
        String id,
        String etapaMataMata,
        Integer chaveIndex,
        String tipoPartida,
        boolean realizada,
        JogadorClubeResumoDTO mandante,
        JogadorClubeResumoDTO visitante,
        Integer golsMandante,
        Integer golsVisitante,
        Integer placarAgregadoMandante,
        Integer placarAgregadoVisitante,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        boolean houvePenaltis
) {}