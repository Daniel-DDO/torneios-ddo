package com.ddo.torneios.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DadosPartidaDTO {
    private String nomeMandante;
    private String timeMandante;
    private String nomeVisitante;
    private String timeVisitante;
    private String relatoOcorrido;
}