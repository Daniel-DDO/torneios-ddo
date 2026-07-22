package com.ddo.torneios.dto;

import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.TipoPartida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PartidaDetalheProjection(
        String id,
        String faseId,
        String rodadaId,
        Integer numeroRodada,
        FaseMataMata etapaMataMata,
        Integer chaveIndex,
        LocalDateTime dataHora,
        String estadio,
        String linkPartida,
        JogadorClubeDTO mandante,
        JogadorClubeDTO visitante,
        Integer golsMandante,
        Integer golsVisitante,
        boolean realizada,
        boolean wo,
        boolean houveProrrogacao,
        Integer penaltisMandante,
        Integer penaltisVisitante,
        String logEventos,
        Integer cartoesAmarelosMandante,
        Integer cartoesVermelhosMandante,
        Integer cartoesAmarelosVisitante,
        Integer cartoesVermelhosVisitante,
        BigDecimal coeficienteMandante,
        BigDecimal coeficienteVisitante,
        TipoPartida tipoPartida,
        String proximaPartidaId,
        Integer slotNaProxima,
        BigDecimal receitaMandante,
        BigDecimal receitaVisitante
) {
}