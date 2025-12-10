package com.ddo.torneios.dto;

import com.ddo.torneios.model.Temporada;

import java.time.LocalDate;

public record TemporadaDTO (
    String id,
    String nome,
    LocalDate dataInicio,
    LocalDate dataFim,
    boolean ativa
    ) {
    public TemporadaDTO(Temporada temporada) {
        this (
                temporada.getId(),
                temporada.getNome(),
                temporada.getDataInicio(),
                temporada.getDataFim(),
                temporada.isAtiva()
        );
    }
}