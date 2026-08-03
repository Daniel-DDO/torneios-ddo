package com.ddo.torneios.model;

import lombok.Getter;

@Getter
public enum RankJogador {
    SEM_RANK  ("Sem Ranking",     0,   199,     120,     30,     -30),
    BRONZE    ("Bronze",        200,   499,     110,     25,     -40),
    PRATA     ("Prata",         500,   899,     100,     25,     -45),
    OURO      ("Ouro",          900,  1399,      95,     20,     -50),
    PLATINA   ("Platina",      1400,  1999,      90,     20,     -55),
    DIAMANTE  ("Diamante",     2000,  2699,      85,     15,     -60),
    CHAMPION  ("Champion",     2700, Integer.MAX_VALUE, 80, 15,  -65);

    private final String nomeExibicao;
    private final int pontosMinimos;
    private final int pontosMaximos;
    private final int pontosVitoria;
    private final int pontosEmpate;
    private final int pontosDerrota;

    RankJogador(String nomeExibicao, int pontosMinimos, int pontosMaximos,
                int pontosVitoria, int pontosEmpate, int pontosDerrota) {
        this.nomeExibicao = nomeExibicao;
        this.pontosMinimos = pontosMinimos;
        this.pontosMaximos = pontosMaximos;
        this.pontosVitoria = pontosVitoria;
        this.pontosEmpate = pontosEmpate;
        this.pontosDerrota = pontosDerrota;
    }

    public int getPontosPorResultado(ResultadoPartida resultado) {
        return switch (resultado) {
            case VITORIA -> pontosVitoria;
            case EMPATE -> pontosEmpate;
            case DERROTA -> pontosDerrota;
        };
    }

    public int getPontosParaProximoRank(int pontosAtuais) {
        if (this == CHAMPION) return 0;
        return Math.max(0, (pontosMaximos + 1) - pontosAtuais);
    }

    public static RankJogador porPontos(int pontos) {
        int pontosClamped = Math.max(pontos, 0);
        for (RankJogador rank : values()) {
            if (pontosClamped >= rank.pontosMinimos && pontosClamped <= rank.pontosMaximos) {
                return rank;
            }
        }
        return CHAMPION;
    }
}