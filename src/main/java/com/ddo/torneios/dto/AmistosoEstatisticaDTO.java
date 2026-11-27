package com.ddo.torneios.dto;

public record AmistosoEstatisticaDTO(
        Long jogos,
        Long vitorias,
        Long empates,
        Long derrotas,
        Long golsMarcados,
        Long golsSofridos,
        Long cartoesAmarelos,
        Long cartoesVermelhos
) {}