package com.ddo.torneios.dto;

import com.ddo.torneios.model.Emprestimo;
import com.ddo.torneios.model.StatusEmprestimo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmprestimoPublicoDTO(
        String jogadorId,
        String jogadorNome,
        String jogadorDiscord,
        String jogadorImagem,
        BigDecimal valorSolicitado,
        BigDecimal valorTotalComJuros,
        BigDecimal valorPago,
        BigDecimal valorRestante,
        Integer quantidadeParcelas,
        Integer parcelasPagas,
        StatusEmprestimo status,
        LocalDateTime dataContratacao
) {
    public static EmprestimoPublicoDTO de(Emprestimo e) {
        BigDecimal valorPago = e.getValorParcela()
                .multiply(BigDecimal.valueOf(e.getParcelasPagas()));
        BigDecimal valorRestante = e.getValorTotalComJuros().subtract(valorPago);

        return new EmprestimoPublicoDTO(
                e.getJogador().getId(),
                e.getJogador().getNome(),
                e.getJogador().getDiscord(),
                e.getJogador().getImagem(),
                e.getValorSolicitado(),
                e.getValorTotalComJuros(),
                valorPago,
                valorRestante,
                e.getQuantidadeParcelas(),
                e.getParcelasPagas(),
                e.getStatus(),
                e.getDataContratacao()
        );
    }
}