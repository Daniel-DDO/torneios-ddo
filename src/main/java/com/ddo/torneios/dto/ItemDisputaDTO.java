package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemDisputaDTO(
        String nomeJogador,
        BigDecimal valorOfertado,
        Integer prioridadeEscolhida,
        LocalDateTime dataLance
) {}