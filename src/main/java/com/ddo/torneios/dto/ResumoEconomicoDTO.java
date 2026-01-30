package com.ddo.torneios.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResumoEconomicoDTO {
    private BigDecimal cotacaoAtual;
    private BigDecimal variacaoPercentual;
    private String tendencia;
    private String mensagem;
    private String corIndicativa;
}