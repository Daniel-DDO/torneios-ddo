package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record ConquistaDashboardDTO(
        String idConquista,
        String idTitulo,
        String nomeTitulo,
        String nomeEdicao,
        String imagemConquista,
        String idJogador,
        String nomeJogador,
        String imagemJogador,
        String idClube,
        String nomeClube,
        String siglaClube,
        String imagemClube,

        LocalDateTime dataHora
) {}