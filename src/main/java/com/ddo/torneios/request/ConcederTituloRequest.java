package com.ddo.torneios.request;

public record ConcederTituloRequest(
        String jogadorClubeId,
        String idTitulo,
        String edicao
) {}