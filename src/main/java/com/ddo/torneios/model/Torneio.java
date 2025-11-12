package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "competicao_id")
    private Competicao competicao;

    private String edicao;
}
