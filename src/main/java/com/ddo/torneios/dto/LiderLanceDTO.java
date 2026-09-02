package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LiderLanceDTO(
        String jogadorId,
        String jogadorNome,
        BigDecimal valor,
        LocalDateTime dataHoraLance
) {}