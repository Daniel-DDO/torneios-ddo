package com.ddo.torneios.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ReportPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    private Partida partida;

    @Column(columnDefinition = "TEXT")
    private String relatoAdmin;

    @Column(columnDefinition = "TEXT")
    private String analiseIA;

    @Column(columnDefinition = "TEXT")
    private String vereditoSugrido;

    private Integer nivelConfiabilidade; //0 a 100%

    private LocalDateTime dataReport = LocalDateTime.now();
}