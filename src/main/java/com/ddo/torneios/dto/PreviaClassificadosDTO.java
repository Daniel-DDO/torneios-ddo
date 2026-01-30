package com.ddo.torneios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PreviaClassificadosDTO {
    private String idFaseAnterior;
    private String nomeFaseAnterior;
    private int quantidadeClassificados;
    private List<ResumoClassificado> classificados;

    @Data
    @AllArgsConstructor
    public static class ResumoClassificado {
        private int posicao;
        private String idJogadorClube;
        private String nomeJogador;
        private String nomeClube;
        private String fotoUrl;
    }
}