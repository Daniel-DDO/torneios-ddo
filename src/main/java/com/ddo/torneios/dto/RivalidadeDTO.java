package com.ddo.torneios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RivalidadeDTO {
    private String adversarioId;
    private String adversarioNome;
    private String adversarioDiscord;
    private String adversarioImagem;

    private int partidasJogadas;
    private int minhasVitorias;
    private int meusEmpates;
    private int minhasDerrotas;

    private int golsFeitos;
    private int golsSofridos;
    private int saldoGols;

    private String aproveitamento;
}