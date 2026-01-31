package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record LanceDetalheDTO(
        String id,
        String clubeId,
        String nomeClube,
        String imagemClube,
        BigDecimal lanceMinimo,
        BigDecimal valor,
        Integer prioridade
) {}