package com.ddo.torneios.repository;

import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Partida;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    boolean existsByFaseAndRealizadaTrue(FaseTorneio fase);

    @Modifying
    @Query("DELETE FROM Partida p WHERE p.fase = :fase AND p.rodada IS NULL")
    void deleteByFaseAndRodadaIsNull(@Param("fase") FaseTorneio fase);

    List<Partida> findByFaseAndRealizadaTrue(FaseTorneio fase);

    @Query("SELECT COUNT(p) = 2 FROM Partida p WHERE p.fase = :fase AND p.etapaMataMata = :etapa AND p.chaveIndex = :chave AND p.realizada = true")
    boolean isConfrontoCompleto(@Param("fase") FaseTorneio fase, @Param("etapa") FaseMataMata etapa, @Param("chave") Integer chave);

    List<Partida> findByFaseAndEtapaMataMataAndChaveIndex(FaseTorneio fase, FaseMataMata etapa, Integer chave);

    List<Partida> findByFase(FaseTorneio fase);
}
