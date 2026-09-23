package com.ddo.torneios.dto;

import com.ddo.torneios.model.Emprestimo;
import com.ddo.torneios.model.StatusEmprestimo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EmprestimoDTO(
        String id,
        String jogadorId,
        String jogadorNome,
        BigDecimal valorSolicitado,
        BigDecimal percentualJuros,
        BigDecimal valorTotalComJuros,
        BigDecimal valorParcela,
        Integer quantidadeParcelas,
        Integer parcelasPagas,
        StatusEmprestimo status,
        LocalDateTime dataContratacao,
        LocalDateTime dataQuitacao,
        List<ParcelaDTO> parcelas
) {
    public static EmprestimoDTO de(Emprestimo e) {
        return new EmprestimoDTO(
                e.getId(),
                e.getJogador().getId(),
                e.getJogador().getNome(),
                e.getValorSolicitado(),
                e.getPercentualJuros(),
                e.getValorTotalComJuros(),
                e.getValorParcela(),
                e.getQuantidadeParcelas(),
                e.getParcelasPagas(),
                e.getStatus(),
                e.getDataContratacao(),
                e.getDataQuitacao(),
                e.getParcelas() == null ? List.of() :
                        e.getParcelas().stream().map(ParcelaDTO::de).toList()
        );
    }
}