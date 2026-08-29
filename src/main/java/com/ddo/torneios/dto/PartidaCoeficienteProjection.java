package com.ddo.torneios.dto;

import java.math.BigDecimal;

public interface PartidaCoeficienteProjection {
    String getId();
    Integer getGolsMandante();
    Integer getGolsVisitante();
    BigDecimal getEstrelasMandante();
    BigDecimal getEstrelasVisitante();
    Integer getValorCompeticao();
}