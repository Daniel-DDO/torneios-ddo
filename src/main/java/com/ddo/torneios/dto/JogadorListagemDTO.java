package com.ddo.torneios.dto;

import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.RankJogador;
import com.ddo.torneios.model.StatusJogador;

import java.math.BigDecimal;

public record JogadorListagemDTO(
        String id,
        String nome,
        String discord,
        String imagem,
        Cargo cargo,
        StatusJogador statusJogador,
        boolean contaReivindicada,
        Integer partidasJogadas,
        Integer vitorias,
        Integer empates,
        Integer derrotas,
        Integer titulos,
        BigDecimal pontosCoeficiente,
        RankJogador rank
) {}