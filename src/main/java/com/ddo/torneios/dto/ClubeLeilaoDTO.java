package com.ddo.torneios.dto;

import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.LigaClube;

import java.math.BigDecimal;

public record ClubeLeilaoDTO(
        String id,
        String nome,
        String nomeExtenso,
        String imagem,
        String sigla,
        BigDecimal lanceMinimo,
        BigDecimal valorAvaliado,
        LigaClube ligaClube,
        BigDecimal estrelas
) {
    public ClubeLeilaoDTO(Clube clube) {
        this(
                clube.getId(),
                clube.getNome(),
                clube.getNomeExtenso(),
                clube.getImagem(),
                clube.getSigla(),
                clube.getLanceMinimo(),
                clube.getValorAvaliado(),
                clube.getLigaClube(),
                clube.getEstrelas()
        );
    }
}