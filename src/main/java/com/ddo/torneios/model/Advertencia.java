package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "advertencia", indexes = {
        @Index(name = "idx_advertencia_jogador_nivel_status", columnList = "jogador_id, nivel, status, valida_ate"),
        @Index(name = "idx_advertencia_multa_pendente", columnList = "multa_debitada, prazo_recurso_ate"),
        @Index(name = "idx_advertencia_suspensao", columnList = "suspensao_id")
})
public class Advertencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private Jogador jogador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelAdvertencia nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAdvertencia status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fato;

    @Column(nullable = false, length = 100)
    private String artigo;

    @Column(columnDefinition = "TEXT")
    private String fundamentacao;

    @Column(nullable = false)
    private Integer multiplicador;

    @Column(name = "multa_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal multaFinal;

    @Column(name = "oficializada_em", nullable = false)
    private OffsetDateTime oficializadaEm;

    @Column(name = "valida_ate", nullable = false)
    private OffsetDateTime validaAte;

    @Column(name = "prazo_recurso_ate", nullable = false)
    private OffsetDateTime prazoRecursoAte;

    @Column(name = "aplicada_por_id", nullable = false)
    private String aplicadaPorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspensao_id")
    private SuspensaoPorAdvertencia suspensao;

    @Column(name = "multa_debitada", nullable = false)
    private boolean multaDebitada = false;

    @Column(name = "valor_debitado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorDebitado = BigDecimal.ZERO;

    @Column(name = "debitada_em")
    private OffsetDateTime debitadaEm;

    @Column(name = "negativou_jogador", nullable = false)
    private boolean negativouJogador = false;

    @Column(name = "recurso_interposto", nullable = false)
    private boolean recursoInterposto = false;

    @Column(name = "recurso_texto", columnDefinition = "TEXT")
    private String recursoTexto;

    @Column(name = "recurso_em")
    private OffsetDateTime recursoEm;

    @Column(name = "recurso_decidido_em")
    private OffsetDateTime recursoDecididoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurso_resultado", length = 20)
    private ResultadoRecurso recursoResultado;

    @Column(name = "recurso_fundamentacao", columnDefinition = "TEXT")
    private String recursoFundamentacao;

    @Column(name = "cancelada_em")
    private OffsetDateTime canceladaEm;

    @Column(name = "cancelada_por_id")
    private String canceladaPorId;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;
}
