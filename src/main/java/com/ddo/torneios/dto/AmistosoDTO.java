package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmistosoDTO(
        String id,
        AmistosoLadoDTO mandante,
        AmistosoLadoDTO visitante,
        LocalDateTime dataHora,
        boolean realizada,
        boolean wo,
        Integer golsMandante,
        Integer golsVisitante,
        int cartoesAmarelosMandante,
        int cartoesVermelhosMandante,
        int cartoesAmarelosVisitante,
        int cartoesVermelhosVisitante,
        BigDecimal receitaMandante,
        BigDecimal receitaVisitante,
        String linkPartida,
        LocalDateTime criadoEm,
        LocalDateTime registradoEm
) {}