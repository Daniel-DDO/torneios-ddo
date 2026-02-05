package com.ddo.torneios.dto;

import com.ddo.torneios.model.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransacaoResponseDTO(
        Long id,
        TipoTransacao tipo,
        BigDecimal valor,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        String motivo,
        String responsavel,
        LocalDateTime dataHora
) {}