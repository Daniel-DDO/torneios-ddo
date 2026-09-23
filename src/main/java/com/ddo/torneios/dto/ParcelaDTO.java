package com.ddo.torneios.dto;

import com.ddo.torneios.model.ParcelaEmprestimo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParcelaDTO(
        String id,
        String emprestimoId,
        Integer numeroParcela,
        BigDecimal valor,
        LocalDateTime dataVencimento,
        LocalDateTime dataPagamento,
        boolean paga,
        boolean pagaComSaldoNegativo
) {
    public static ParcelaDTO de(ParcelaEmprestimo p) {
        return new ParcelaDTO(
                p.getId(),
                p.getEmprestimo().getId(),
                p.getNumeroParcela(),
                p.getValor(),
                p.getDataVencimento(),
                p.getDataPagamento(),
                p.isPaga(),
                p.isPagaComSaldoNegativo()
        );
    }
}