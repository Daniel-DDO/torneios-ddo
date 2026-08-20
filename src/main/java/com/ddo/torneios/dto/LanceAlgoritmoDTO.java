package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LanceAlgoritmoDTO(
        String id,
        String jogadorId,
        String jogadorNome,
        String clubeId,
        String clubeNome,
        String clubeImagem,
        BigDecimal valor,
        Integer prioridade,
        LocalDateTime dataHoraLance
) {}