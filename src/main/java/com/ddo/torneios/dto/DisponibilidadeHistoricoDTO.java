package com.ddo.torneios.dto;

import com.ddo.torneios.model.DiaSemana;
import com.ddo.torneios.model.TurnoDisponibilidade;
import com.ddo.torneios.util.DisponibilidadeMascara;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Uma versão da grade no log do perfil. */
public record DisponibilidadeHistoricoDTO(
        Long id,
        Map<DiaSemana, List<TurnoDisponibilidade>> grade,
        LocalDateTime criadoEm
) {
    /** Usado pela query JPQL (SELECT new), que entrega a máscara crua. */
    public DisponibilidadeHistoricoDTO(Long id, Integer mascara, LocalDateTime criadoEm) {
        this(id, DisponibilidadeMascara.paraGrade(mascara), criadoEm);
    }
}
