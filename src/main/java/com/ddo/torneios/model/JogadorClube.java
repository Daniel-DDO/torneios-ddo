package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"jogador_id", "temporada_id"})
})
public class JogadorClube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "jogador_id")
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "clube_id")
    private Clube clube;

    @ManyToOne
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    private BigDecimal balancoFinanceiro;

    private Long cartoesAmarelos;
    private Long cartoesVermelhos;

    @Enumerated(EnumType.STRING)
    private StatusClassificacao statusClassificacao;

    private Integer totalGolsMarcados;
    private Integer totalGolsSofridos;

    @ColumnDefault("0.00")
    private BigDecimal pontosCoeficiente;
}