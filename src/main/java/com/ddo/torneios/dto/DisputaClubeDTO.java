package com.ddo.torneios.dto;

import java.util.List;

public record DisputaClubeDTO(
        String clubeId,
        String clubeNome,
        String imagemClube,
        Integer totalInteressados,
        List<ItemDisputaDTO> ranking
) {}

