package com.ddo.torneios.model;

public enum FaseMataMata {
    FINAL(2),
    SEMIFINAL(4),
    QUARTAS(8),
    OITAVAS(16),
    DEZESSEIS_AVOS(32),
    TRINTA_E_DOIS_AVOS(64);

    private final int numeroTimes;

    FaseMataMata(int numeroTimes) {
        this.numeroTimes = numeroTimes;
    }

    public int getNumeroTimes() {
        return numeroTimes;
    }
}
