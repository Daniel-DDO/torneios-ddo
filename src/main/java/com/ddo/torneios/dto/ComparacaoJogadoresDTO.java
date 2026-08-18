package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComparacaoJogadoresDTO(
        DadosJogadorComparacao jogador1,
        DadosJogadorComparacao jogador2,
        List<PartidaHistoricoDTO> confrontosDiretos,
        ResumoConfrontoDiretoDTO resumoConfrontoDireto
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
            BigDecimal saldo,
            BigDecimal pontosCoeficiente
    ) {}
}