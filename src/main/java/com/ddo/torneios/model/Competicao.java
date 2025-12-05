package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
public class Competicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String imagem;
    private String divisao;

    private Integer valor; //valor da competição (peso) de 0 a 100.

}
