package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "leilao_id")
    private Leilao leilao;

    @ManyToOne
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "clube_id")
    private Clube clube;

    private BigDecimal valorPago;

    private LocalDateTime dataCompra;

    public Transferencia(Leilao leilao, Jogador jogador, Clube clube, BigDecimal valorPago) {
        this.leilao = leilao;
        this.jogador = jogador;
        this.clube = clube;
        this.valorPago = valorPago;
        this.dataCompra = LocalDateTime.now();
    }
}