package com.ddo.torneios.dto;

import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.StatusClassificacao;

import java.math.BigDecimal;

public record JogadorClubeDTO(
        String id,
        String jogadorId,
        String jogadorNome,
        String jogadorImagem,
        String clubeId,
        String clubeNome,
        String clubeImagem,
        String clubeSigla,
        String temporadaId,
        String temporadaNome,
        Integer golsMarcados,
        Integer golsSofridos,
        Integer jogos,
        BigDecimal pontosCoeficiente,
        StatusClassificacao statusTemporada
) {
    public JogadorClubeDTO(JogadorClube entidade) {
        this(
                entidade.getId(),
                entidade.getJogador().getId(),
                entidade.getJogador().getNome(),
                entidade.getJogador().getImagem(),
                entidade.getClube().getId(),
                entidade.getClube().getNome(),
                entidade.getClube().getImagem(),
                entidade.getClube().getSigla(),
                entidade.getTemporada().getId(),
                entidade.getTemporada().getNome(),
                entidade.getTotalGolsMarcados(),
                entidade.getTotalGolsSofridos(),
                entidade.getPartidasJogadas(),
                entidade.getPontosCoeficiente(),
                entidade.getStatusTemporada()
        );
    }
}