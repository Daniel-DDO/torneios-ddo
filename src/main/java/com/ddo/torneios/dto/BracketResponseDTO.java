package com.ddo.torneios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class BracketResponseDTO {
    private String estadioFinal;
    private Map<String, List<PartidaDTO>> partidas;
}