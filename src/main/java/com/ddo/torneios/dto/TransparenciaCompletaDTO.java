package com.ddo.torneios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransparenciaCompletaDTO {
    private ParametrosEconomicosDTO economico;
    private ParametrosCoeficienteDTO coeficiente;
}