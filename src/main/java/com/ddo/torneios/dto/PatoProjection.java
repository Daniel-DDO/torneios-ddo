package com.ddo.torneios.dto;

public interface PatoProjection {
    String getAdversarioId();
    String getAdversarioNome();
    String getAdversarioDiscord();
    String getAdversarioImagem();
    Integer getTotalJogos();
    Integer getMinhasVitorias();
    Integer getMeusEmpates();
    Integer getMeusGols();
    Integer getGolsSofridos();
}