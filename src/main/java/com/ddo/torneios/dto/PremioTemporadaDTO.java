package com.ddo.torneios.dto;

import com.ddo.torneios.model.CategoriaPremio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PremioTemporadaDTO(
        String id,
        CategoriaPremio categoria,
        String jogadorId,
        String jogadorNome,
        BigDecimal valorEstatistica,
        LocalDateTime dataApuracao
) {}