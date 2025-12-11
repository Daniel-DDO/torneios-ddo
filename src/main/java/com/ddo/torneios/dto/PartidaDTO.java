package com.ddo.torneios.dto;

import com.ddo.torneios.model.Partida;

import java.time.LocalDateTime;

public record PartidaDTO(
        String id,
        String faseId,

        //dados da liga (pode ser null em mata-mata)
        String rodadaId,
        Integer numeroRodada,

        //dados do mata-mata (pode ser null em liga)
        String etapaMataMata,
        Integer chaveIndex,

        LocalDateTime dataHora,
        String estadio,
        String linkPartida,

        //times
        JogadorClubeDTO mandante,
        JogadorClubeDTO visitante,

        //placar
        Integer golsMandante,
        Integer golsVisitante,

        //flags
        boolean realizada,
        boolean wo,
        boolean houveProrrogacao,
        boolean houvePenaltis,

        //pênaltis (achatados para facilitar o front)
        Integer penaltisMandante,
        Integer penaltisVisitante,

        String logEventos,

        Integer cartoesAmarelosMandante,
        Integer cartoesVermelhosMandante,
        Integer cartoesAmarelosVisitante,
        Integer cartoesVermelhosVisitante
) {
    public PartidaDTO(Partida p) {
        this(
                p.getId(),
                p.getFase().getId(),

                p.getRodada() != null ? p.getRodada().getId() : null,
                p.getRodada() != null ? p.getRodada().getNumero() : null,

                p.getEtapaMataMata() != null ? p.getEtapaMataMata().name() : null,
                p.getChaveIndex(),

                p.getDataHora(),
                p.getEstadio(),
                p.getLinkPartida(),

                new JogadorClubeDTO(p.getMandante()),
                new JogadorClubeDTO(p.getVisitante()),

                p.getGolsMandante(),
                p.getGolsVisitante(),

                p.isRealizada(),
                p.isWo(),
                p.isHouveProrrogacao(),
                p.houvePenaltis(),

                p.houvePenaltis() ? p.getPenaltis().getGolsMandante() : null,
                p.houvePenaltis() ? p.getPenaltis().getGolsVisitante() : null,

                p.getLogEventos(),

                p.getCartoesAmarelosMandante(),
                p.getCartoesVermelhosMandante(),
                p.getCartoesAmarelosVisitante(),
                p.getCartoesVermelhosVisitante()
        );
    }
}