package com.ddo.torneios.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParametrosCoeficienteDTO {
    private double tetoGols;
    private double pontosVitoria;
    private double pontosEmpate;
    private double pontosGoleada;
    private double pontosCleanSheet;
    private double pontosDerrota;
    private double penalidadePorAmarelo;
    private int limiteAmarelosSemPunicao;
    private double penalidadePorVermelho;
    private double penalidadePorGolSofrido;
    private double divisorNivelTime;
    private double pontuacaoMinima;
    private double pontuacaoMaxima;
}