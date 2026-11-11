package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "suspensao_por_advertencia", indexes = {
        @Index(name = "idx_suspensao_advertencia_jogador", columnList = "jogador_id")
})
public class SuspensaoPorAdvertencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSuspensaoAdvertencia tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemSuspensaoAdvertencia origem;

    @Column(name = "criada_em", nullable = false)
    private OffsetDateTime criadaEm;

    @Column(nullable = false)
    private boolean cancelada = false;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;
}
