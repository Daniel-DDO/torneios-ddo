package com.ddo.torneios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MovimentacaoSaldoDTO(
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotBlank String motivo,
        @NotNull TipoOperacao operacao,

        boolean confirmarSaldoNegativo
) {
    public enum TipoOperacao { ADICIONAR, REMOVER }
}