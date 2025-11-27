package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private boolean ativa;

    @OneToMany(mappedBy = "temporada", cascade = CascadeType.ALL)
    private List<Torneio> torneios;
}