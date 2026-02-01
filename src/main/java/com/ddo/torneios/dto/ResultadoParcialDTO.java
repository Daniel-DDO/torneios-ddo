package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record ResultadoParcialDTO(
        String nomeClube,
        String imagemClube,
        String nomeVencedor,
        BigDecimal valor,
        Integer prioridade
) {}