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
@Table(name = "parcela_emprestimo")
public class ParcelaEmprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id")
    private Emprestimo emprestimo;

    @NotNull
    private Integer numeroParcela;

    @NotNull
    private BigDecimal valor;

    @NotNull
    private LocalDateTime dataVencimento;

    private LocalDateTime dataPagamento;

    @NotNull
    private boolean paga = false;

    /** Marca se essa parcela específica foi paga deixando o jogador negativado */
    @NotNull
    private boolean pagaComSaldoNegativo = false;

    public ParcelaEmprestimo() {}
}