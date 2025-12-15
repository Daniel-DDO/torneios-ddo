package com.ddo.torneios.dto;

import java.util.List;

public record RelatorioFaseDTO(
        String torneioNome,
        String faseNome,
        List<LinhaClassificacaoDTO> classificacao,
        List<RodadaPdfDTO> rodadas,
        List<AgendaJogadorDTO> agendaJogadores
) {}