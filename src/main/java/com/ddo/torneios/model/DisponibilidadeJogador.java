package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma versão da grade de disponibilidade do jogador. Append-only: cada alteração
 * gera uma nova linha e a "atual" é a de maior criadoEm. Sem setters de propósito.
 *
 * mascara: 28 bits, bit = diaOrdinal * 4 + turnoOrdinal (ver DisponibilidadeMascara).
 * criadoEm: sempre horário de Brasília.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "disponibilidade_jogador", indexes = {
        @Index(name = "idx_disp_jogador_criado_em", columnList = "jogador_id, criado_em")
})
public class DisponibilidadeJogador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogador_id", nullable = false)
    private String jogadorId;

    @Column(nullable = false, updatable = false)
    private int mascara;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public DisponibilidadeJogador(String jogadorId, int mascara, LocalDateTime criadoEm) {
        this.jogadorId = jogadorId;
        this.mascara = mascara;
        this.criadoEm = criadoEm;
    }
}
