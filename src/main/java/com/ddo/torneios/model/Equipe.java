package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"torneio_id", "nome"}),
        indexes = @Index(name = "idx_equipe_torneio", columnList = "torneio_id")
)
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @Column(length = 500)
    private String imagem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capitao_id", nullable = false)
    private Jogador capitao;

    @Override
    public String toString() {
        return "Equipe{id='" + id + "'}";
    }
}