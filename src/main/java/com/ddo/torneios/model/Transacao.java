package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "transacao")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @NotNull
    private BigDecimal valor;

    private BigDecimal saldoAnterior;
    private BigDecimal saldoPosterior;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    private String responsavel;

    @NotNull
    private LocalDateTime dataHora;

    public Transacao(Jogador jogador, TipoTransacao tipo, BigDecimal valor, BigDecimal saldoAnterior, BigDecimal saldoPosterior, String motivo, String responsavel) {
        this.jogador = jogador;
        this.tipo = tipo;
        this.valor = valor;
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoPosterior;
        this.motivo = motivo;
        this.responsavel = responsavel;
        this.dataHora = LocalDateTime.now();
    }

    public Transacao() {}
}