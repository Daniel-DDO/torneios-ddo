package com.ddo.torneios.request;

public record AtualizarParticipacaoFaseRequest(
        Integer pontos,
        Integer partidasJogadas,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer golsPro,
        Integer golsContra
) {}