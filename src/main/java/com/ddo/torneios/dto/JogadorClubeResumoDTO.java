package com.ddo.torneios.dto;

import com.ddo.torneios.model.JogadorClube;

public record JogadorClubeResumoDTO(
        String id,
        String jogadorId,
        String jogadorNome,
        String jogadorImagem,
        String clubeId,
        String clubeNome,
        String clubeImagem,
        String clubeSigla
) {
    public JogadorClubeResumoDTO(JogadorClube jc) {
        this(
                jc.getId(),
                jc.getJogador().getId(),
                jc.getJogador().getNome(),
                jc.getJogador().getImagem(),
                jc.getClube().getId(),
                jc.getClube().getNome(),
                jc.getClube().getImagem(),
                jc.getClube().getSigla()
        );
    }
}