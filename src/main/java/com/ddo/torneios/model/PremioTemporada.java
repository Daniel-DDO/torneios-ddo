package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"temporada_id", "categoria"})
})
public class PremioTemporada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaPremio categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    private String jogadorNomeSnapshot; //nome no momento da apuração

    private BigDecimal valorEstatistica; //gols, índice ou pontos, conforme a categoria

    private LocalDateTime dataApuracao;

    @PrePersist
    public void prePersist() {
        if (dataApuracao == null) dataApuracao = LocalDateTime.now();
    }
}