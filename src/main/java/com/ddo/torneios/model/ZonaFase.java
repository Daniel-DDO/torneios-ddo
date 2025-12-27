package com.ddo.torneios.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ZonaFase {
    private String nome;
    private Integer posicaoDe;
    private Integer posicaoAte;
    private String corHex;
}