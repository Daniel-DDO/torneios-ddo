package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    @ManyToOne
    @JoinColumn(name = "competicao_id")
    private Competicao competicao;

    @Enumerated(EnumType.STRING)
    private TipoTorneio tipo;

    private Integer ordem;

    @OneToMany(mappedBy = "torneio")
    private List<ParticipacaoTorneio> participacoes;
}
