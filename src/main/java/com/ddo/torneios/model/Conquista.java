package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "titulo_id")
    private Titulo titulo;

    private LocalDate dataConquista;
    private String nomeEdicao;

    public Conquista(Titulo titulo, String nomeEdicao) {
        this.titulo = titulo;
        this.dataConquista = LocalDate.now();
        this.nomeEdicao = nomeEdicao;
    }
}