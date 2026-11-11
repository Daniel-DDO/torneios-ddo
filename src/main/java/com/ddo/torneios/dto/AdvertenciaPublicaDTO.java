package com.ddo.torneios.dto;

import com.ddo.torneios.model.Advertencia;
import com.ddo.torneios.model.NivelAdvertencia;
import com.ddo.torneios.model.StatusAdvertencia;
import com.ddo.torneios.util.RelogioBrasilia;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdvertenciaPublicaDTO(
        String id,
        OffsetDateTime data,
        String fato,
        String artigo,
        NivelAdvertencia nivel,
        BigDecimal multa,
        Integer multiplicador,
        String fundamentacao,
        StatusAdvertencia status,
        OffsetDateTime validaAte
) {

    public AdvertenciaPublicaDTO {
        data = RelogioBrasilia.paraBrasilia(data);
        validaAte = RelogioBrasilia.paraBrasilia(validaAte);
    }

    public static AdvertenciaPublicaDTO de(Advertencia advertencia) {
        return new AdvertenciaPublicaDTO(
                advertencia.getId(),
                advertencia.getOficializadaEm(),
                advertencia.getFato(),
                advertencia.getArtigo(),
                advertencia.getNivel(),
                advertencia.getMultaFinal(),
                advertencia.getMultiplicador(),
                advertencia.getFundamentacao(),
                advertencia.getStatus(),
                advertencia.getValidaAte()
        );
    }
}
