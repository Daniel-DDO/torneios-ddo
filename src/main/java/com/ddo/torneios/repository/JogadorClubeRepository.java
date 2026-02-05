package com.ddo.torneios.repository;

import com.ddo.torneios.model.JogadorClube;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JogadorClubeRepository extends JpaRepository<JogadorClube, String> {
    boolean existsByJogadorIdAndTemporadaId(String jogadorId, String temporadaId);
    boolean existsByClubeIdAndTemporadaId(String clubeId, String temporadaId);
    List<JogadorClube> findByTemporadaId(String temporadaId);
    List<JogadorClube> findTop10ByJogadorNomeContainingIgnoreCase(String nome);
    List<JogadorClube> findTop10ByClubeNomeContainingIgnoreCase(String nome);

    @Query("SELECT jc FROM JogadorClube jc " +
            "WHERE jc.temporada.id = :temporadaId " +
            "AND (" +
            "  LOWER(jc.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.jogador.discord) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.clube.sigla) LIKE LOWER(CONCAT('%', :termo, '%'))" +
            ")")
    List<JogadorClube> buscarPorTermoETemporada(
            @Param("termo") String termo,
            @Param("temporadaId") String temporadaId,
            Pageable pageable
    );
}