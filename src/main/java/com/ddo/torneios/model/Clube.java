package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
public class Clube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @NotBlank
    private String estadio;

    @Column(columnDefinition = "TEXT")
    private String imagem;

    @Enumerated(EnumType.STRING)
    private LigaClube ligaClube;
}
