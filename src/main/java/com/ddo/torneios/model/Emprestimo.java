package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "emprestimo")
public class Emprestimo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    /** Valor efetivamente solicitado/liberado ao jogador (sem juros) */
    @NotNull
    private BigDecimal valorSolicitado;

    /** Percentual de juros aplicado, de acordo com a quantidade de parcelas (ex: 10 = 10%) */
    @NotNull
    private BigDecimal percentualJuros;

    /** Valor total a ser pago (valorSolicitado + juros) */
    @NotNull
    private BigDecimal valorTotalComJuros;

    /** Valor de cada parcela semanal (valorTotalComJuros / quantidadeParcelas) */
    @NotNull
    private BigDecimal valorParcela;

    @NotNull
    private Integer quantidadeParcelas;

    @NotNull
    private Integer parcelasPagas = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusEmprestimo status;

    @NotNull
    private LocalDateTime dataContratacao;

    private LocalDateTime dataQuitacao;

    private String responsavelLiberacao;

    @OneToMany(mappedBy = "emprestimo", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("numeroParcela ASC")
    private List<ParcelaEmprestimo> parcelas = new ArrayList<>();

    public Emprestimo() {}
}