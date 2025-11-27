package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

@Data
@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "mandante_id")
    private JogadorClube mandante;

    private Integer golsMandante;
    private Integer golsVisitante;

    @ManyToOne
    @JoinColumn(name = "visitante_id")
    private JogadorClube visitante;

    private boolean realizada;
    private boolean wo;

    private Integer numeroRodada;

    @Enumerated(EnumType.STRING)
    private FaseTorneio faseTorneio;

    @Column(columnDefinition = "TEXT")
    private String logEventos;

    private String linkPartida;

    @ColumnDefault("false")
    private boolean houveProrrogacao;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "golsMandante", column = @Column(name = "penaltis_mandante", nullable = true)),
            @AttributeOverride(name = "golsVisitante", column = @Column(name = "penaltis_visitante", nullable = true))
    })
    private DisputaPenaltis penaltis;

    public boolean houvePenaltis() {
        return penaltis != null;
    }
}
