package com.ddo.torneios.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AtualizarValoresClubeRequest {

    @DecimalMin(value = "0.00", message = "O valor avaliado não pode ser negativo")
    private BigDecimal valorAvaliado;

    @DecimalMin(value = "0.00", message = "O lance mínimo não pode ser negativo")
    private BigDecimal lanceMinimo;
}