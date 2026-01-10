package com.ddo.torneios.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ParametrosEconomicosDTO {
    private BigDecimal cotaTvFixa;
    private BigDecimal valorPorEstrelaBilheteria;
    private BigDecimal premioVitoria;
    private BigDecimal premioEmpate;
    private BigDecimal custoBaseEstrela;
    private BigDecimal bonusZebraPorEstrela;
    private BigDecimal fatorPunicaoGoleada;
    private Integer percentualMinimoCompeticao;
    private String explicacaoFatorCompeticao;

    private BigDecimal premioBaseCampeao;
    private BigDecimal premioBaseVice;
}