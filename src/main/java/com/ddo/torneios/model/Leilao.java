package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Leilao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    private String descricao;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    private boolean ativo;

    @OneToMany(mappedBy = "leilao", cascade = CascadeType.ALL)
    private List<Lance> lances;

    @ColumnDefault("false")
    private boolean selecao;
}