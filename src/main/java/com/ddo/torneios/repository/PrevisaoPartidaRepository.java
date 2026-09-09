package com.ddo.torneios.repository;

import com.ddo.torneios.model.PrevisaoPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrevisaoPartidaRepository extends JpaRepository<PrevisaoPartida, String> {

    @Query("""
        SELECT
            COUNT(p),
            SUM(CASE WHEN p.acertouResultado = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.acertouPlacarExato = true THEN 1 ELSE 0 END)
        FROM PrevisaoPartida p
        """)
    Object[] buscarResumoAcuracia();

    boolean existsByPartidaId(String partidaId);
}