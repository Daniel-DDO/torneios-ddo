package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record JogadorHistoriaDTO(
        String id,
        String nome,
        String imagem,
        String cargo,
        Integer totalJogos,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        String aproveitamento,
        Integer golsMarcados,
        Integer golsSofridos,
        Integer saldoGols,
        Double mediaGolsPorJogo,
        Integer titulos,
        Integer finais,
        BigDecimal pontosCoeficiente
) {}