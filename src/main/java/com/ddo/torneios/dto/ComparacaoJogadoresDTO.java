package com.ddo.torneios.dto;

public record ComparacaoJogadoresDTO(
        DadosJogadorComparacao jogador1,
        DadosJogadorComparacao jogador2
) {
    public record DadosJogadorComparacao(
            String id,
            String nome,
            String discord,
            String imagem,
            Integer titulos,
            Integer finais,
            Integer partidasJogadas,
            Integer vitorias,
            Integer golsMarcados,
            Integer golsSofridos,
            String aproveitamento,
            java.math.BigDecimal saldo,
            java.math.BigDecimal pontosCoeficiente
    ) {}
}