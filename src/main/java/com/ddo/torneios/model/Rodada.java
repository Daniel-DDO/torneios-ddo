package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fase_id", "numero"})
})
public class Rodada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Integer numero;

    private String nome;

    @ManyToOne
    @JoinColumn(name = "fase_id", nullable = false)
    private FaseTorneio fase;

    private LocalDateTime dataInicioPrevista;
    private LocalDateTime dataFimPrevista;

    @Enumerated(EnumType.STRING)
    private StatusRodada status;

    @OneToMany(mappedBy = "rodada", cascade = CascadeType.ALL)
    private List<Partida> partidas;

    public boolean isCompleta() {
        if (partidas == null || partidas.isEmpty()) return false;
        return partidas.stream().allMatch(Partida::isRealizada);
    }
}