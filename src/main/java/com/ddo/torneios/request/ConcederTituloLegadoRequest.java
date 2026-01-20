package com.ddo.torneios.request;

import java.time.LocalDateTime;

public record ConcederTituloLegadoRequest(
        String jogadorId,
        String clubeId,
        String idTitulo,
        String edicao,
        LocalDateTime data
) {}