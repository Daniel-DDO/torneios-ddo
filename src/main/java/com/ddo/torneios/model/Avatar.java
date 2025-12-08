package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private String nome;

    public Avatar() {}

    public Avatar(String nome, String url) {
        this.nome = nome;
        this.url = url;
    }
}