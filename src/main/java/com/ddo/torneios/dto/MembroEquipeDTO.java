package com.ddo.torneios.dto;

public record MembroEquipeDTO(String equipeId, String participacaoId, String jogadorId, String nome,
                              String clubeNome, Boolean capitao, Integer pontos, Integer partidasJogadas,
                              Integer vitorias, Integer empates, Integer derrotas, Integer golsPro,
                              Integer golsContra, Integer saldoGols, Integer posicao) {}

