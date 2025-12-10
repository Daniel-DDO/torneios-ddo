package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Data
@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fase_id", nullable = false)
    private FaseTorneio fase;

    //só é usado para fase de liga ou grupos, em mata-mata é null
    private Integer numeroRodada;

    //só é usado para o mata-mata, se for fase de liga, é null
    @Enumerated(EnumType.STRING)
    private FaseMataMata etapaMataMata;

    private LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "mandante_id", nullable = false)
    private JogadorClube mandante;

    @ManyToOne
    @JoinColumn(name = "visitante_id", nullable = false)
    private JogadorClube visitante;

    private Integer golsMandante;
    private Integer golsVisitante;

    @ColumnDefault("false")
    private boolean realizada;

    @ColumnDefault("false")
    private boolean wo;

    @ColumnDefault("false")
    private boolean houveProrrogacao;

    private String estadio;

    @Column(columnDefinition = "TEXT")
    private String logEventos;

    private String linkPartida;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "golsMandante", column = @Column(name = "penaltis_mandante")),
            @AttributeOverride(name = "golsVisitante", column = @Column(name = "penaltis_visitante"))
    })
    private DisputaPenaltis penaltis;

    public boolean houvePenaltis() {
        return penaltis != null &&
                penaltis.getGolsMandante() != null &&
                penaltis.getGolsVisitante() != null;
    }

    public JogadorClube getVencedor() {
        if (!realizada) return null;

        if (wo) {
            if (golsMandante > golsVisitante) return mandante;
            if (golsVisitante > golsMandante) return visitante;
        }

        if (golsMandante > golsVisitante) return mandante;
        if (golsVisitante > golsMandante) return visitante;

        if (houvePenaltis()) {
            if (penaltis.getGolsMandante() > penaltis.getGolsVisitante()) return mandante;
            if (penaltis.getGolsVisitante() > penaltis.getGolsMandante()) return visitante;
        }

        return null;
    }
}