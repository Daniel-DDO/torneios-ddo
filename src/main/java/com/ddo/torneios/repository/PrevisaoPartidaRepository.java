package com.ddo.torneios.repository;

import com.ddo.torneios.dto.ResumoAcuraciaDTO;
import com.ddo.torneios.model.PrevisaoPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrevisaoPartidaRepository extends JpaRepository<PrevisaoPartida, String> {

    @Query("""
    SELECT new com.ddo.torneios.dto.ResumoAcuraciaDTO(
        COUNT(p),
        SUM(CASE WHEN p.acertouResultado = true THEN 1L ELSE 0L END),
        SUM(CASE WHEN p.acertouPlacarExato = true THEN 1L ELSE 0L END)
    )
    FROM PrevisaoPartida p
    """)
    ResumoAcuraciaDTO buscarResumoAcuracia();

    boolean existsByPartidaId(String partidaId);
}