package com.ddo.torneios.repository;

import com.ddo.torneios.model.Partida;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, String> {
    List<Partida> findByFaseIdOrderByDataHoraAsc(String faseId);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.id = :faseId " +
            "AND (p.mandante.id = :jogadorClubeId OR p.visitante.id = :jogadorClubeId) " +
            "ORDER BY p.dataHora ASC")
    List<Partida> findByJogadorNaFase(@Param("faseId") String faseId,
                                      @Param("jogadorClubeId") String jogadorClubeId);

    @Query("SELECT p FROM Partida p " +
            "WHERE LOWER(p.mandante.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.visitante.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.mandante.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.visitante.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Partida> buscarAutocomplete(@Param("termo") String termo, Pageable pageable);

    List<Partida> findByFaseId(String faseId);
    List<Partida> findByRodadaId(String rodadaId);
}
