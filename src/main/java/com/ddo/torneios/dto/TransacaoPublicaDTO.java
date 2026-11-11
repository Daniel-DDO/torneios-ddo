package com.ddo.torneios.dto;

import com.ddo.torneios.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoPublicaDTO(
        Long id,
        String jogadorId,
        String jogadorNome,
        TipoTransacao tipo,
        BigDecimal valor,
        String motivo,
        LocalDateTime dataHora
) {
}
