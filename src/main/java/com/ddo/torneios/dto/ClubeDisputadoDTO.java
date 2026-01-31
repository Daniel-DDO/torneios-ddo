package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record ClubeDisputadoDTO(
        String idClube,
        String nomeClube,
        String imagemClube,
        Long totalLances,
        BigDecimal maiorLanceAtual
) {}