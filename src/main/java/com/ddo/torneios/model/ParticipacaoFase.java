package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    private Integer posicaoClassificacao;

    private String grupo; //para fase de grupos

    @ElementCollection
    @CollectionTable(name = "participacao_fase_historico_jc", joinColumns = @JoinColumn(name = "participacao_fase_id"))
    @Column(name = "jogador_clube_id_antigo")
    private List<String> historicoJogadorClubeIds = new ArrayList<>();

    @Override
    public String toString() {
        return "ParticipacaoFase{id='" + id + "'}";
    }
}