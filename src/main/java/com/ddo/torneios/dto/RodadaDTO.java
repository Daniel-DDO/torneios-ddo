package com.ddo.torneios.dto;

import com.ddo.torneios.model.Rodada;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record RodadaDTO(
        String id,
        Integer numero,
        String nome,
        String faseId,
        String status, //AGENDADA, EM_ANDAMENTO...
        LocalDateTime dataInicioPrevista,
        LocalDateTime dataFimPrevista,
        boolean completa,
        List<PartidaDTO> partidas
) {
    public RodadaDTO(Rodada r) {
        this(
                r.getId(),
                r.getNumero(),
                r.getNome(),
                r.getFase().getId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getDataInicioPrevista(),
                r.getDataFimPrevista(),
                r.isCompleta(),
                r.getPartidas() != null ?
                        r.getPartidas().stream()
                                .sorted(Comparator.comparing(p -> p.getDataHora() != null ? p.getDataHora() : LocalDateTime.MAX))
                                .map(PartidaDTO::new)
                                .collect(Collectors.toList())
                        : List.of()
        );
    }
}