package com.ddo.torneios.dto;

import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.model.StatusClassificacao;

public record ParticipacaoFaseDTO(
        String id,
        String faseId,
        String faseNome,
        String jogadorClubeId,
        String jogadorNome,
        String clubeNome,
        String clubeSigla,
        String clubeImagem,
        Integer pontos,
        Integer partidasJogadas,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsPro,
        Integer golsContra,
        Integer saldoGols,
        StatusClassificacao statusClassificacao
) {
    public ParticipacaoFaseDTO(ParticipacaoFase entidade) {
        this(
                entidade.getId(),
                entidade.getFase().getId(),
                entidade.getFase().getNome(),
                entidade.getJogadorClube().getId(),
                entidade.getJogadorClube().getJogador().getNome(),
                entidade.getJogadorClube().getClube().getNome(),
                entidade.getJogadorClube().getClube().getSigla(),
                entidade.getJogadorClube().getClube().getImagem(),
                entidade.getPontos(),
                entidade.getPartidasJogadas(),
                entidade.getVitorias(),
                entidade.getEmpates(),
                entidade.getDerrotas(),
                entidade.getGolsPro(),
                entidade.getGolsContra(),
                entidade.getSaldoGols(),
                entidade.getStatusClassificacao()
        );
    }
}