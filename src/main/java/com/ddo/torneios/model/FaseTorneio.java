package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"torneio_id", "ordem"})
})
public class FaseTorneio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String nome;

    @Column(nullable = false)
    private Integer ordem; //1, 2, 3...

    @ManyToOne
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTorneio tipoTorneio; //PONTOS_CORRIDOS ou MATA_MATA...

    @Column(name = "numero_rodadas")
    private Integer numeroRodadas;

    @OrderBy("numero ASC")
    @OneToMany(mappedBy = "fase", cascade = CascadeType.ALL)
    private List<Rodada> rodadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_inicial_mata_mata")
    private FaseMataMata faseInicialMataMata;

    @Column(name = "tem_jogo_volta")
    private Boolean temJogoVolta;

    @OneToMany(mappedBy = "fase", cascade = CascadeType.ALL)
    private List<ParticipacaoFase> participacoes;

    @Enumerated(EnumType.STRING)
    private AlgoritmoGeracaoLiga algoritmoLiga;

    @Enumerated(EnumType.STRING)
    private AlgoritmoGeracaoMataMata algoritmoMataMata;

    //Configurações paramétricas
    private Integer maxJogosEmCasa;

    @ElementCollection
    @CollectionTable(name = "fase_zonas", joinColumns = @JoinColumn(name = "fase_id"))
    private List<ZonaFase> zonas;

    @Column(name = "final_jogo_unico")
    private Boolean finalJogoUnico = true;

    @PrePersist
    @PreUpdate
    private void validarConsistencia() {
        if (TipoTorneio.PONTOS_CORRIDOS.equals(this.tipoTorneio)) {
            if (this.numeroRodadas == null || this.numeroRodadas <= 0) {
                throw new IllegalStateException("Fases de pontos corridos exigem número de rodadas válido.");
            }
        } else if (TipoTorneio.MATA_MATA.equals(this.tipoTorneio)) {
            if (this.faseInicialMataMata == null) {
                throw new IllegalStateException("Fases de mata-mata exigem uma fase inicial definida (ex: OITAVAS).");
            }
        }
    }
}