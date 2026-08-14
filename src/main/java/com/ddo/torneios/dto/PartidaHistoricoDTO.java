package com.ddo.torneios.dto;

import com.ddo.torneios.model.Partida;

import java.time.LocalDateTime;

public record PartidaHistoricoDTO(
        String id,
        String faseId,
        String rodadaId,
        Integer numeroRodada,
        LocalDateTime dataHora,
        String estadio,
        JogadorClubeResumoDTO mandante,
        JogadorClubeResumoDTO visitante,
        Integer golsMandante,
        Integer golsVisitante,
        boolean realizada,
        boolean wo,
        boolean houvePenaltis,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        boolean anulada,
        String motivoAnulacao
) {
    public PartidaHistoricoDTO(Partida p) {
        this(
                p.getId(),
                p.getFase().getId(),
                p.getRodada() != null ? p.getRodada().getId() : null,
                p.getRodada() != null ? p.getRodada().getNumero() : null,
                p.getDataHora(),
                p.getEstadio(),
                p.getMandante() != null ? new JogadorClubeResumoDTO(p.getMandante()) : null,
                p.getVisitante() != null ? new JogadorClubeResumoDTO(p.getVisitante()) : null,
                p.getGolsMandante(),
                p.getGolsVisitante(),
                p.isRealizada(),
                p.isWo(),
                p.houvePenaltis(),
                (p.houvePenaltis() && p.getPenaltis() != null) ? p.getPenaltis().getGolsMandante() : null,
                (p.houvePenaltis() && p.getPenaltis() != null) ? p.getPenaltis().getGolsVisitante() : null,
                p.isAnulada(),
                p.getMotivoAnulacao()
        );
    }
}