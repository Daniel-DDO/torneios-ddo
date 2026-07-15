package com.ddo.torneios.dto;

public record ParticipacaoClassificacaoProjection(
        String id,
        String jogadorClubeId,
        String nomeJogador,
        String nomeClube,
        String imagemClube
) {}