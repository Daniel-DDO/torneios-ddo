package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fase_id", "jogador_clube_id"})
})
public class ParticipacaoFase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "fase_id", nullable = false)
    private FaseTorneio fase;

    @ManyToOne
    @JoinColumn(name = "jogador_clube_id", nullable = false)
    private JogadorClube jogadorClube;

    private Integer pontos = 0;
    private Integer partidasJogadas = 0;
    private Integer vitorias = 0;
    private Integer empates = 0;
    private Integer derrotas = 0;
    private Integer golsPro = 0;
    private Integer golsContra = 0;
    private Integer saldoGols = 0;

    @Enumerated(EnumType.STRING)
    private StatusClassificacao statusClassificacao;
}