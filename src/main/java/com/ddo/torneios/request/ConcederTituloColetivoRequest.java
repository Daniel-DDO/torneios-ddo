package com.ddo.torneios.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConcederTituloColetivoRequest {
    private List<String> jogadoresIds;
    private String clubeId;
    private String idTitulo;
    private String edicao;
    private LocalDateTime data;
}