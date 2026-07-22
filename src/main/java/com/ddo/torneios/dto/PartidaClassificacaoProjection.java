package com.ddo.torneios.dto;

public record PartidaClassificacaoProjection(
        String mandanteId,
        String visitanteId,
        Integer golsMandante,
        Integer golsVisitante,
        Integer cartoesAmarelosMandante,
        Integer cartoesVermelhosMandante,
        Integer cartoesAmarelosVisitante,
        Integer cartoesVermelhosVisitante
) {}