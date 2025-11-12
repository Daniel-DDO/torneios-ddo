package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class JogadorClube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "clube_id")
    private Clube clube;

    @ManyToOne
    @JoinColumn(name = "torneio_id")
    private Torneio torneio;
}
