package com.ddo.torneios.dto;

import com.ddo.torneios.model.Partida;

import java.math.BigDecimal;
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
        Integer cartoesVermelhosVisitante,

        BigDecimal coeficienteMandante,
        BigDecimal coeficienteVisitante,

        String tipoPartida,

        String proximaPartidaId,
        Integer slotNaProxima,

        BigDecimal receitaMandante,
        BigDecimal receitaVisitante,

        Integer placarAgregadoMandante,
        Integer placarAgregadoVisitante,

        boolean anulada,
        String motivoAnulacao,
        LocalDateTime anuladaEm
) {
    public PartidaDTO(Partida atual, Partida ida) {
        this(
                atual.getId(),
                atual.getFase().getId(),
                atual.getRodada() != null ? atual.getRodada().getId() : null,
                atual.getRodada() != null ? atual.getRodada().getNumero() : null,
                atual.getEtapaMataMata() != null ? atual.getEtapaMataMata().name() : null,
                atual.getChaveIndex(),
                atual.getDataHora(),
                atual.getEstadio(),
                atual.getLinkPartida(),
                atual.getMandante() != null ? new JogadorClubeDTO(atual.getMandante()) : null,
                atual.getVisitante() != null ? new JogadorClubeDTO(atual.getVisitante()) : null,
                atual.getGolsMandante(),
                atual.getGolsVisitante(),
                atual.isRealizada(),
                atual.isWo(),
                atual.isHouveProrrogacao(),
                atual.houvePenaltis(),
                (atual.houvePenaltis() && atual.getPenaltis() != null) ? atual.getPenaltis().getGolsMandante() : null,
                (atual.houvePenaltis() && atual.getPenaltis() != null) ? atual.getPenaltis().getGolsVisitante() : null,
                atual.getLogEventos(),
                atual.getCartoesAmarelosMandante(),
                atual.getCartoesVermelhosMandante(),
                atual.getCartoesAmarelosVisitante(),
                atual.getCartoesVermelhosVisitante(),
                atual.getCoeficienteMandante(),
                atual.getCoeficienteVisitante(),
                atual.getTipoPartida() != null ? atual.getTipoPartida().name() : null,
                atual.getProximaPartida() != null ? atual.getProximaPartida().getId() : null,
                atual.getSlotNaProxima(),
                atual.getReceitaMandante() != null ? atual.getReceitaMandante() : BigDecimal.ZERO,
                atual.getReceitaVisitante() != null ? atual.getReceitaVisitante() : BigDecimal.ZERO,

                calcularAgregadoMandante(atual, ida),
                calcularAgregadoVisitante(atual, ida),

                atual.isAnulada(),
                atual.getMotivoAnulacao(),
                atual.getAnuladaEm()
        );
    }

    public PartidaDTO(Partida p) {
        this(p, null);
    }

    private static Integer calcularAgregadoMandante(Partida atual, Partida ida) {
        if (atual.getGolsMandante() == null) return null;
        if (ida == null || ida.getGolsVisitante() == null) return atual.getGolsMandante(); // Se não tem ida, o agregado é o jogo atual

        if (ida.getVisitante().equals(atual.getMandante())) {
            return atual.getGolsMandante() + ida.getGolsVisitante();
        }
        return atual.getGolsMandante() + ida.getGolsMandante();
    }

    private static Integer calcularAgregadoVisitante(Partida atual, Partida ida) {
        if (atual.getGolsVisitante() == null) return null;
        if (ida == null || ida.getGolsMandante() == null) return atual.getGolsVisitante();

        if (ida.getMandante().equals(atual.getVisitante())) {
            return atual.getGolsVisitante() + ida.getGolsMandante();
        }
        return atual.getGolsVisitante() + ida.getGolsVisitante();
    }
}