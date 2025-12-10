package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TemporadaRequest {
    @NotBlank
    private String nome;

    private LocalDate dataInicio;
    private LocalDate dataFim;
}
