package com.ddo.torneios.service.radar;

import com.ddo.torneios.dto.AtributosRadarDTO;
import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.dto.LigaMediasDTO;

public class RadarAtributosCalculator {

    private static final double PESO_CARTAO_AMARELO = 2.0;
    private static final double PESO_CARTAO_VERMELHO = 5.0;

    private static final int NOTA_MIN = 0;
    private static final int NOTA_MAX = 100;

    /** Evita divisão por zero quando a liga ainda não tem dados suficientes. */
    private static final double MEDIA_MINIMA = 0.01;

    /**
     * @param mediasLiga médias reais da liga (ponderadas por partidas jogadas, não
     *                    "média das médias") — servem de referência dinâmica para
     *                    todos os atributos. Nenhum atributo usa teto fixo: um jogador
     *                    exatamente na média da liga sempre tira 50 no atributo.
     */
    public AtributosRadarDTO calcular(JogadorDTO j, LigaMediasDTO mediasLiga) {
        int jogos = jogosValidos(j);

        return new AtributosRadarDTO(
                clamp(calcularNotaAtaque(j, jogos, mediasLiga)),
                clamp(calcularNotaDefesa(j, jogos, mediasLiga)),
                clamp(calcularNotaEficiencia(j, jogos)),
                clamp(calcularNotaDisciplina(j, jogos, mediasLiga)),
                clamp(calcularNotaExperiencia(jogos, mediasLiga))
        );
    }

    /** Maior é melhor: nota = 100 * valor / (valor + média da liga). */
    private int calcularNotaAtaque(JogadorDTO j, int jogos, LigaMediasDTO medias) {
        double mediaGolsPro = valorOuZero(j.golsMarcados()) / (double) jogos;
        return notaRelativaPositiva(mediaGolsPro, medias.mediaGolsProPorJogo());
    }

    /** Menor é melhor: nota = 100 * média da liga / (média da liga + valor). */
    private int calcularNotaDefesa(JogadorDTO j, int jogos, LigaMediasDTO medias) {
        double mediaGolsContra = valorOuZero(j.golsSofridos()) / (double) jogos;
        return notaRelativaInvertida(mediaGolsContra, medias.mediaGolsContraPorJogo());
    }

    /** Taxa de vitória já é uma proporção natural (0 a 1), não precisa de referência externa. */
    private int calcularNotaEficiencia(JogadorDTO j, int jogos) {
        double taxaVitoria = valorOuZero(j.vitorias()) / (double) jogos;
        return (int) Math.round(taxaVitoria * NOTA_MAX);
    }

    /** Menor é melhor: mesma lógica da defesa, aplicada ao "custo" ponderado de cartões. */
    private int calcularNotaDisciplina(JogadorDTO j, int jogos, LigaMediasDTO medias) {
        long amarelos = valorOuZero(j.cartoesAmarelos());
        long vermelhos = valorOuZero(j.cartoesVermelhos());
        double mediaCartoes = ((amarelos * PESO_CARTAO_AMARELO) + (vermelhos * PESO_CARTAO_VERMELHO)) / jogos;
        return notaRelativaInvertida(mediaCartoes, medias.mediaCartoesPorJogo());
    }

    /** Maior é melhor: mesma fórmula do ataque, mas usando volume de jogos como valor. */
    private int calcularNotaExperiencia(int jogos, LigaMediasDTO medias) {
        return notaRelativaPositiva(jogos, medias.mediaJogos());
    }

    private int notaRelativaPositiva(double valor, double mediaLiga) {
        double base = Math.max(mediaLiga, MEDIA_MINIMA);
        double nota = NOTA_MAX * (valor / (valor + base));
        return (int) Math.round(nota);
    }

    private int notaRelativaInvertida(double valor, double mediaLiga) {
        double base = Math.max(mediaLiga, MEDIA_MINIMA);
        double nota = NOTA_MAX * (base / (base + valor));
        return (int) Math.round(nota);
    }

    private int jogosValidos(JogadorDTO j) {
        return (j.partidasJogadas() != null && j.partidasJogadas() > 0) ? j.partidasJogadas() : 1;
    }

    private int valorOuZero(Integer v) {
        return v != null ? v : 0;
    }

    private long valorOuZero(Long v) {
        return v != null ? v : 0L;
    }

    private int clamp(int valor) {
        return Math.max(NOTA_MIN, Math.min(NOTA_MAX, valor));
    }
}