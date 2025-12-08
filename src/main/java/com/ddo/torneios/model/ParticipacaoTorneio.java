package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"torneio_id", "jogador_clube_id"})
})
public class ParticipacaoTorneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "torneio_id")
    private Torneio torneio;

    @ManyToOne
    @JoinColumn(name = "jogador_clube_id")
    private JogadorClube jogadorClube;

    private Integer pontos = 0;
    private Integer vitorias = 0;
    private Integer empates = 0;
    private Integer derrotas = 0;
    private Integer golsPro = 0;
    private Integer golsContra = 0;
    private Integer saldoGols = 0;
    private Double aproveitamento = 0.00;

    @Enumerated(EnumType.STRING)
    private StatusClassificacao statusClassificacao;
}