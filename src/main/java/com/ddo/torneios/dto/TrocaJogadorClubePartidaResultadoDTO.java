package com.ddo.torneios.dto;

import java.util.List;

public record TrocaJogadorClubePartidaResultadoDTO(
        String jogadorClubeNovoId,
        boolean jogadorClubeNovoCriado,
        boolean participacaoFaseCriada,
        List<String> partidasAtualizadas
) {}