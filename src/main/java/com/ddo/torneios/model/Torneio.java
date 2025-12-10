package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"temporada_id", "competicao_id"})
})
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;

    @ManyToOne
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @ManyToOne
    @JoinColumn(name = "competicao_id", nullable = false)
    private Competicao competicao;

    @OneToMany(mappedBy = "torneio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FaseTorneio> fases;
}