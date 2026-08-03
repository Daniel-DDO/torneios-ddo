package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_conquista_titulo_edicao_jogador",
        columnNames = {"titulo_id", "nomeEdicao", "jogador_id"}
))
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "titulo_id", nullable = false)
    private Titulo titulo;

    private LocalDateTime dataConquista;
    private String nomeEdicao;

    @Column(columnDefinition = "TEXT")
    private String imagem;

    @ManyToOne
    @JoinColumn(name = "clube_id")
    @JsonIgnore
    private Clube clube;

    @ManyToOne
    @JoinColumn(name = "jogador_id")
    @JsonIgnore
    private Jogador jogador;

    public Conquista(Titulo titulo, String nomeEdicao, Clube clube, Jogador jogador) {
        this.titulo = titulo;
        this.nomeEdicao = nomeEdicao;
        this.clube = clube;
        this.jogador = jogador;
        this.dataConquista = LocalDateTime.now();
    }
}