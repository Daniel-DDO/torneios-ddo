package com.ddo.torneios.dto;

public record PartidaPdfDTO(
        String mandanteClube,
        String mandanteJogador,
        String mandanteImagem,
        String visitanteClube,
        String visitanteJogador,
        String visitanteImagem,
        Integer golsMandante,
        Integer golsVisitante,
        String estadio,
        boolean realizada
) {}