package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record RecordePartidaDTO(
        String partidaId,
        JogadorClubeResumoDTO mandante,
        JogadorClubeResumoDTO visitante,
        int golsMandante, int golsVisitante,
        LocalDateTime dataHora, String estadio
) {}
