package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InflacaoMercadoDTO(
        boolean aplicada,
        String motivo,
        BigDecimal mediaSaldoJogadores,
        BigDecimal medianaSaldoJogadores,
        BigDecimal indicadorAtual,
        BigDecimal indicadorAnterior,
        BigDecimal crescimentoPercentual,
        BigDecimal multiplicadorAplicado,
        Integer clubesAtualizados,
        LocalDateTime dataCalculo
) {}