package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Registro de imprevisto do jogador. Append-only, com timestamp em horário de Brasília. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "observacao_disponibilidade", indexes = {
        @Index(name = "idx_obs_disp_jogador_criado_em", columnList = "jogador_id, criado_em")
})
public class ObservacaoDisponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogador_id", nullable = false)
    private String jogadorId;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public ObservacaoDisponibilidade(String jogadorId, String texto, LocalDateTime criadoEm) {
        this.jogadorId = jogadorId;
        this.texto = texto;
        this.criadoEm = criadoEm;
    }
}
