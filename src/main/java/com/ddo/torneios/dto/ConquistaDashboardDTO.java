package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record ConquistaDashboardDTO(
        String idConquista,
        String nomeTitulo,
        String nomeEdicao,
        String imagemConquista,
        String nomeJogador,
        String imagemJogador,
        String nomeClube,
        String siglaClube,
        String imagemClube,
        LocalDateTime dataHora 
) {}