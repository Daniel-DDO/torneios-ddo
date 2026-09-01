package com.ddo.torneios.dto;

import com.ddo.torneios.model.TipoPartida;

public record PartidaTrocaProjection(
        String id,
        String faseId,
        TipoPartida tipoPartida,
        boolean realizada,
        String mandanteId,
        String visitanteId,
        String proximaPartidaId
) {}