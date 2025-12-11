package com.ddo.torneios.repository;

import com.ddo.torneios.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, String> {
    List<Partida> findByFaseIdOrderByDataHoraAsc(String faseId);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.id = :faseId " +
            "AND (p.mandante.id = :jogadorClubeId OR p.visitante.id = :jogadorClubeId) " +
            "ORDER BY p.dataHora ASC")
    List<Partida> findByJogadorNaFase(@Param("faseId") String faseId,
                                      @Param("jogadorClubeId") String jogadorClubeId);
}
