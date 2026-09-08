package com.ddo.torneios.dto;

import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.TipoPartida;

import java.math.BigDecimal;

public record PartidaProbabilidadeDTO(
        String id,
        boolean realizada,
        String faseId,
        Integer chaveIndex,
        TipoPartida tipoPartida,
        FaseMataMata etapaMataMata,

        String mandanteJogadorId,
        String mandanteJogadorNome,
        Integer mandantePartidasJogadas,
        Integer mandanteVitorias,
        Integer mandanteGolsMarcados,
        Integer mandanteGolsSofridos,
        Integer mandantePartidasJogadasNaTemporada,
        Double mandanteAproveitamentoTemporada,
        Integer mandanteGolsMarcadosTemporada,
        Integer mandanteGolsSofridosTemporada,
        BigDecimal mandanteClubeEstrelas,

        String visitanteJogadorId,
        String visitanteJogadorNome,
        Integer visitantePartidasJogadas,
        Integer visitanteVitorias,
        Integer visitanteGolsMarcados,
        Integer visitanteGolsSofridos,
        Integer visitantePartidasJogadasNaTemporada,
        Double visitanteAproveitamentoTemporada,
        Integer visitanteGolsMarcadosTemporada,
        Integer visitanteGolsSofridosTemporada,
        BigDecimal visitanteClubeEstrelas
) {}