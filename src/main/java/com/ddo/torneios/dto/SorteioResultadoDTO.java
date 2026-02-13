package com.ddo.torneios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SorteioResultadoDTO {
    private String jogadorId;
    private String jogadorNome;
    private String clubeId;
    private String clubeNome;
    private String clubeImagem;
}