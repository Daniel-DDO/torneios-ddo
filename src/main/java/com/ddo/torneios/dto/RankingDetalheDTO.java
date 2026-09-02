package com.ddo.torneios.dto;

public record RankingDetalheDTO(
        String jogadorId,
        String nomeJogador,
        String rankAtual,
        int pontosAtuais,
        boolean emColocacao,
        int partidasFaltantesColocacao,
        int pontosParaProximoRank,
        int pontosDeColchaoAntesDoRebaixamento,
        int strikesRebaixamento,
        int strikesParaRebaixar
) {}