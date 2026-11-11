package com.ddo.torneios.dto;

import com.ddo.torneios.model.NivelAdvertencia;
import com.ddo.torneios.util.RelogioBrasilia;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdvertenciaRecursoDTO(
        String id,
        String jogadorId,
        String jogadorNome,
        NivelAdvertencia nivel,
        String fato,
        String artigo,
        Integer multiplicador,
        BigDecimal multaFinal,
        OffsetDateTime oficializadaEm,
        String recursoTexto,
        OffsetDateTime recursoEm
) {

    public AdvertenciaRecursoDTO {
        oficializadaEm = RelogioBrasilia.paraBrasilia(oficializadaEm);
        recursoEm = RelogioBrasilia.paraBrasilia(recursoEm);
    }
}
