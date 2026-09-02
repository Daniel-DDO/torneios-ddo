package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MercadoStatusDTO(
        LocalDateTime ultimaExecucao,
        LocalDateTime ultimaExecucaoComSucesso,
        BigDecimal ultimaVariacaoUsdAplicada,
        BigDecimal ultimaCotacaoUsd,
        Integer clubesAtualizadosUltimaExecucao,
        boolean ultimaExecucaoComErro,
        String ultimoErro,
        LocalDateTime ultimaExecucaoIpca,
        BigDecimal ultimaVariacaoIpcaAplicada
) {}