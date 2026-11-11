package com.ddo.torneios.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum NivelAdvertencia {

    LEVE(new BigDecimal("1000.00"), 20, 5),
    MEDIA(new BigDecimal("3000.00"), 20, 3),
    GRAVE(new BigDecimal("5000.00"), 20, 2),
    GRAVISSIMA(new BigDecimal("7000.00"), 40, 0);

    private final BigDecimal multaBase;
    private final int validadeDias;
    private final int limiteConversao;

    public boolean convertePorContagem() {
        return limiteConversao > 0;
    }
}
