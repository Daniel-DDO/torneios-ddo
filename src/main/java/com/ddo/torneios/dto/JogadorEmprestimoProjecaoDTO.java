package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JogadorEmprestimoProjecaoDTO(
        String id,
        String nome,
        Integer partidasJogadas,
        BigDecimal saldoVirtual,
        boolean negativado,
        LocalDateTime dataQuitacaoNegativacao
) {}