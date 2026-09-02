package com.ddo.torneios.dto;

import java.math.BigDecimal;

public record ClubeBasicoDTO(
        String id,
        String nome,
        String imagem,
        BigDecimal lanceMinimo
) {}