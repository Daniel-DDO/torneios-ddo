package com.ddo.torneios.dto;

import com.ddo.torneios.model.ReportPartida;
import java.time.LocalDateTime;

public record ReportPartidaDTO(
        String id,
        PartidaDTO partida,
        String relatoAdmin,
        String analiseIA,
        String vereditoSugerido,
        Integer nivelConfiabilidade,
        LocalDateTime dataReport
) {
    public ReportPartidaDTO(ReportPartida entity) {
        this(
                entity.getId(),
                new PartidaDTO(entity.getPartida()),
                entity.getRelatoAdmin(),
                entity.getAnaliseIA(),
                entity.getVereditoSugrido(),
                entity.getNivelConfiabilidade(),
                entity.getDataReport()
        );
    }
}