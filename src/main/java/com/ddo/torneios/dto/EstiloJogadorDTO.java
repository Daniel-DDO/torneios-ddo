package com.ddo.torneios.dto;

import java.util.List;

public record EstiloJogadorDTO(
        String jogadorId,
        int partidasConsideradas,
        double mediaGolsMarcadosPorJogo,
        double mediaGolsSofridosPorJogo,
        double mediaEstrelasClubes,
        double mediaGolsMarcadosGlobal,
        double mediaGolsSofridosGlobal,
        double mediaEstrelasGlobal,
        String estiloProvavel,
        List<String> caracteristicas
) {}