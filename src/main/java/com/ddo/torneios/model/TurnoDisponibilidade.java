package com.ddo.torneios.model;

import lombok.Getter;

/**
 * Turnos do dia (horário de Brasília). A ordem importa: a posição (ordinal)
 * define o bit do turno dentro do bloco do dia na máscara.
 */
@Getter
public enum TurnoDisponibilidade {
    MADRUGADA("Madrugada", "0h às 5h59", false),
    MANHA("Manhã", "6h às 11h59", true),
    TARDE("Tarde", "12h às 17h59", true),
    NOITE("Noite", "18h às 23h59", true);

    private final String rotulo;
    private final String faixa;
    /** A madrugada pode ser marcada, mas sozinha não faz o dia contar pro mínimo exigido. */
    private final boolean contaParaMinimo;

    TurnoDisponibilidade(String rotulo, String faixa, boolean contaParaMinimo) {
        this.rotulo = rotulo;
        this.faixa = faixa;
        this.contaParaMinimo = contaParaMinimo;
    }
}
