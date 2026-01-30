package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoLancesClubeDTO(
        String jogadorId,
        String nomeJogador,
        String imagemJogador,
        BigDecimal valorLance,
        LocalDateTime dataLance
) {}