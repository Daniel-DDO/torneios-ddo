package com.ddo.torneios.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FeedItemDTO(
        String idJogador,
        String nomeJogador,
        String idClube,
        String nomeClube,
        String imagemClube,
        BigDecimal valor,
        LocalDateTime dataHora
) {}
