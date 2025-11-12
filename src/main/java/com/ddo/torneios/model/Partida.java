package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "mandante_id")
    private JogadorClube mandante;

    private Integer golsMandante;
    private Integer golsVisitante;

    @ManyToOne
    @JoinColumn(name = "visitante_id")
    private JogadorClube visitante;

}
