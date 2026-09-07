package com.ddo.torneios.dto;

public record RadarJogadorDTO(
        String jogadorId,
        String nome,
        String imagem,
        AtributosRadarDTO atributos
) {}