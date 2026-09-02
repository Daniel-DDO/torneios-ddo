package com.ddo.torneios.dto;

import java.time.LocalDate;

public record MelhorTemporadaDTO(
        String temporadaId,
        String temporadaNome,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean temporadaAtiva,

        int partidasJogadas,
        int vitorias,
        int empates,
        int derrotas,
        int golsMarcados,
        int golsSofridos,
        int saldoGols,
        double mediaGolsMarcadosPorJogo,
        double mediaGolsSofridosPorJogo,
        double aproveitamento,
        double score
) {}