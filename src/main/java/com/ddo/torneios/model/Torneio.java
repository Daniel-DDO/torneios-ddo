package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;

    @ManyToOne
    @JoinColumn(name = "temporada_id")
    private Temporada temporada;

    @ManyToOne
    @JoinColumn(name = "competicao_id")
    private Competicao competicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTorneio tipoTorneio;

    private Integer ordem;

    //Pontos Corridos
    @Column(name = "numero_rodadas")
    private Integer numeroRodadas;

    //Mata-Mata
    @Enumerated(EnumType.STRING)
    @Column(name = "fase_inicial")
    private FaseMataMata faseInicial;

    @Column(name = "tem_jogo_volta")
    private Boolean temJogoVolta;

    @OneToMany(mappedBy = "torneio")
    private List<ParticipacaoTorneio> participacoes;

    @PrePersist
    @PreUpdate
    private void validarConsistencia() {
        if (TipoTorneio.PONTOS_CORRIDOS.equals(this.tipoTorneio)) {
            if (this.numeroRodadas == null || this.numeroRodadas <= 0) {
                throw new IllegalStateException("Torneios de pontos corridos exigem número de rodadas válido.");
            }
        } else if (TipoTorneio.MATA_MATA.equals(this.tipoTorneio)) {
            if (this.faseInicial == null) {
                throw new IllegalStateException("Torneios mata-mata exigem uma fase inicial definida.");
            }
        }
    }
}