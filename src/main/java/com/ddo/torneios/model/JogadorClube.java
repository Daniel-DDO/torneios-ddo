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
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "clube_id", nullable = false)
    private Clube clube;

    @ManyToOne
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @ColumnDefault("0.00")
    private BigDecimal balancoFinanceiro;

    @ColumnDefault("0.00")
    private BigDecimal pontosCoeficiente;

    private Integer totalCartoesAmarelos = 0;
    private Integer totalCartoesVermelhos = 0;

    private Integer totalGolsMarcados = 0;
    private Integer totalGolsSofridos = 0;

    private Integer partidasJogadas = 0;
    private Integer vitorias = 0;
    private Integer empates = 0;
    private Integer derrotas = 0;
    private Double aproveitamento = 0.0;

    @Enumerated(EnumType.STRING)
    private StatusClassificacao statusTemporada;
}