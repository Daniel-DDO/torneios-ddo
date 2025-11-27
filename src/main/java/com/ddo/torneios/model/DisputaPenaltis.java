package com.ddo.torneios.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class DisputaPenaltis {
    private Integer golsMandante;
    private Integer golsVisitante;
}