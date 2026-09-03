package com.ddo.torneios.dto;

import com.ddo.torneios.model.LigaClube;

import java.math.BigDecimal;

public record ClubeResumoDTO(
        String id,
        String nome,
        String estadio,
        String imagem,
        LigaClube ligaClube,
        String sigla,
        String corPrimaria,
        String corSecundaria,
        boolean ativo,
        BigDecimal estrelas
) {}