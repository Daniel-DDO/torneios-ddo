package com.ddo.torneios.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"jogador_id", "leilao_id", "prioridade"})
        },
        indexes = {
                @Index(name = "idx_lance_leilao_jogador", columnList = "leilao_id, jogador_id"),
                @Index(name = "idx_lance_leilao_clube_prioridade", columnList = "leilao_id, clube_id, prioridade"),
                @Index(name = "idx_lance_leilao_clube", columnList = "leilao_id, clube_id")
        }
)
public class Lance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "leilao_id", nullable = false)
    private Leilao leilao;

    @ManyToOne
    @JoinColumn(name = "jogador_id", nullable = false)
    @JsonIgnoreProperties({
            "conquistas", "insignias", "historico", "titulos", "codigoReivindicacao", "validadeCodigoReivindicacao",
            "senha", "password", "email", "pin", "authorities", "suspensoAte", "cartoesVermelhos", "cartoesAmarelos", "finais",
            "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled",
            "criacaoConta", "modificacaoConta", "golsMarcados", "golsSofridos", "partidasJogadas", "vitorias", "empates", "derrotas"
    })
    private Jogador jogador;

    @ManyToOne
    @JoinColumn(name = "clube_id", nullable = false)
    @JsonIgnoreProperties({"conquistas", "titulos", "elenco", "historico", "lanceMinimo", "valorAvaliado"})
    private Clube clube;

    @NotNull
    private BigDecimal valor;

    @NotNull
    private Integer prioridade;

    private LocalDateTime dataHoraLance;

    private LocalDateTime dataLance;

    @PrePersist
    public void prePersist() {
        if (this.dataLance == null) {
            this.dataLance = LocalDateTime.now();
        }
    }

    public Lance(Leilao leilao, Jogador jogador, Clube clube, BigDecimal valor, Integer prioridade) {
        this.leilao = leilao;
        this.jogador = jogador;
        this.clube = clube;
        this.valor = valor;
        this.prioridade = prioridade;
        this.dataHoraLance = LocalDateTime.now();
    }
}