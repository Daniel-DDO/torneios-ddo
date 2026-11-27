package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partida amistosa. Totalmente à parte do fluxo oficial: não tem fase, temporada,
 * JogadorClube, coeficiente nem rank, e não altera os contadores do Jogador.
 * Só gera saldo (receita fixa por participante) e é consultada por agregação.
 */
@Getter
@Setter
@Entity
@Table(
        name = "amistoso",
        indexes = {
                @Index(name = "idx_amistoso_jogador_mandante", columnList = "jogador_mandante_id"),
                @Index(name = "idx_amistoso_jogador_visitante", columnList = "jogador_visitante_id"),
                @Index(name = "idx_amistoso_realizada_criado", columnList = "realizada, criado_em")
        }
)
public class Amistoso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_mandante_id", nullable = false)
    private Jogador jogadorMandante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clube_mandante_id", nullable = false)
    private Clube clubeMandante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_visitante_id", nullable = false)
    private Jogador jogadorVisitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clube_visitante_id", nullable = false)
    private Clube clubeVisitante;

    // data combinada para jogar (opcional)
    private LocalDateTime dataHora;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime registradoEm;

    @ColumnDefault("false")
    private boolean realizada;

    @ColumnDefault("false")
    private boolean wo;

    private Integer golsMandante;
    private Integer golsVisitante;

    @ColumnDefault("0")
    private int cartoesAmarelosMandante;
    @ColumnDefault("0")
    private int cartoesVermelhosMandante;
    @ColumnDefault("0")
    private int cartoesAmarelosVisitante;
    @ColumnDefault("0")
    private int cartoesVermelhosVisitante;

    private String linkPartida;

    // quanto já foi creditado a cada lado (permite corrigir resultado sem pagar em duplicidade)
    @Column(nullable = false, precision = 19, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal receitaMandante = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal receitaVisitante = BigDecimal.ZERO;
}