package com.ddo.torneios.dto;

import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.TipoPartida;

public record PartidaBracketProjection(
        String id,
        FaseMataMata etapaMataMata,
        Integer chaveIndex,
        TipoPartida tipoPartida,
        boolean realizada,
        JogadorClubeResumoDTO mandante,
        JogadorClubeResumoDTO visitante,
        Integer golsMandante,
        Integer golsVisitante,
        Integer penaltisMandante,
        Integer penaltisVisitante
) {}