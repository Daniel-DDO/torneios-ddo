package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record LeilaoResumoDTO(
        String id,
        String descricao,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        boolean ativo,
        boolean selecao,
        String temporadaId
) {}