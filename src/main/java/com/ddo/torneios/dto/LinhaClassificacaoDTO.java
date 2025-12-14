package com.ddo.torneios.dto;

public record LinhaClassificacaoDTO(
        Integer posicao,
        String jogadorClubeId,
        String nomeJogador,
        String nomeClube,
        String imagemClube,
        Integer pontos,
        Integer jogos,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsPro,
        Integer golsContra,
        Integer saldoGols,
        String zonaNome,
        String zonaCor
) {}