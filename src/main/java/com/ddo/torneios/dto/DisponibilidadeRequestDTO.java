package com.ddo.torneios.dto;

import com.ddo.torneios.model.DiaSemana;
import com.ddo.torneios.model.TurnoDisponibilidade;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.Set;

/** Ex.: { "grade": { "SEG": ["NOITE"], "SAB": ["MANHA", "NOITE"] } } — dias ausentes = sem disponibilidade. */
public record DisponibilidadeRequestDTO(
        @NotNull Map<DiaSemana, Set<TurnoDisponibilidade>> grade
) {}
