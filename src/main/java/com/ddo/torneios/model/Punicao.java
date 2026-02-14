package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
public class Punicao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "participacao_fase_id", nullable = false)
    private ParticipacaoFase participacaoFase;

    @NotNull
    private Integer pontos;

    @NotBlank
    private String motivo;

    private LocalDateTime dataAplicacao;

    public Punicao(ParticipacaoFase participacaoFase, Integer pontos, String motivo) {
        this.participacaoFase = participacaoFase;
        this.pontos = pontos;
        this.motivo = motivo;
        this.dataAplicacao = LocalDateTime.now();
    }
}