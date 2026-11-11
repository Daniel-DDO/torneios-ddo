package com.ddo.torneios.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FichaAdvertenciasDTO(
        String jogadorId,
        String jogadorNome,
        boolean emRegimeReincidencia,
        OffsetDateTime regimeReincidenciaAte,
        List<ContagemNivelDTO> contagens,
        List<AdvertenciaPublicaDTO> advertenciasAtivas
) {
}
