package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Jogador {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @NotBlank
    private String discord;

    private String email;
    private String senha;
    private Integer finais;
    private Integer titulos;
    private Integer golsMarcados;
    private Integer golsSofridos;
    private Integer partidasJogadas;

    @NotNull
    private LocalDateTime criacaoConta;

    @NotNull
    private LocalDateTime modificacaoConta;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    private boolean contaReivindicada;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Cargo cargo;

    @Column(columnDefinition = "TEXT")
    private String imagem;

    @Column(columnDefinition = "TEXT")
    private String descricao;
    private Integer pin;

    private LocalDateTime suspensoAte;

    public Jogador(String nome, String discord) {
        this.id = UUID.randomUUID().toString();
        this.nome = nome;
        this.discord = discord;
        this.finais = 0;
        this.titulos = 0;
        this.golsMarcados = 0;
        this.golsSofridos = 0;
        this.partidasJogadas = 0;
        this.criacaoConta = LocalDateTime.now();
        this.modificacaoConta = LocalDateTime.now();
        this.status = Status.ATIVO;
        this.contaReivindicada = false;
        this.cargo = Cargo.JOGADOR;
    }

    public Jogador() {

    }
}
