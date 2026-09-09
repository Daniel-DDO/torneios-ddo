package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = "partida_id"),
        indexes = @Index(name = "idx_previsao_partida", columnList = "partida_id")
)
public class PrevisaoPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "partida_id", nullable = false, unique = true)
    private Partida partida;

    // Previsão calculada pelo ProbabilidadeService, no instante do registro do resultado
    private int chanceMandantePrevista;
    private int chanceEmpatePrevista;
    private int chanceVisitantePrevista;

    private int golsMandanteCotado;
    private int golsVisitanteCotado;
    private double expectativaGolsMandante;
    private double expectativaGolsVisitante;

    // Resultado real, registrado no mesmo instante (já sabido, pois a previsão só é
    // calculada quando o admin está confirmando o resultado)
    private int golsMandanteReal;
    private int golsVisitanteReal;

    // Acurácia — já resolvida na gravação, sem necessidade de job posterior
    private boolean acertouResultado;      // V/E/D bateu?
    private boolean acertouPlacarExato;    // placar cotado bateu exatamente?

    private LocalDateTime calculadaEm;
}