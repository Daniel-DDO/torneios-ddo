package com.ddo.torneios.dto;

import com.ddo.torneios.model.DiaSemana;
import com.ddo.torneios.model.TurnoDisponibilidade;
import com.ddo.torneios.util.DisponibilidadeMascara;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DisponibilidadeDTO(
        String jogadorId,
        Map<DiaSemana, List<TurnoDisponibilidade>> grade,
        LocalDateTime atualizadoEm,
        LocalDateTime proximaAlteracaoEm,
        boolean podeAlterarAgora
) {
    /** Jogador que nunca preencheu a grade. */
    public static DisponibilidadeDTO vazia(String jogadorId) {
        return new DisponibilidadeDTO(jogadorId, DisponibilidadeMascara.paraGrade(0), null, null, true);
    }

    public static DisponibilidadeDTO de(DisponibilidadeAtualProjecaoDTO projecao, Duration janelaAlteracao, LocalDateTime agora) {
        LocalDateTime proxima = projecao.criadoEm().plus(janelaAlteracao);
        return new DisponibilidadeDTO(
                projecao.jogadorId(),
                DisponibilidadeMascara.paraGrade(projecao.mascara()),
                projecao.criadoEm(),
                proxima,
                !agora.isBefore(proxima)
        );
    }
}
