package com.ddo.torneios.repository;

import com.ddo.torneios.dto.PremioTemporadaDTO;
import com.ddo.torneios.model.CategoriaPremio;
import com.ddo.torneios.model.PremioTemporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PremioTemporadaRepository extends JpaRepository<PremioTemporada, String> {

    Optional<PremioTemporada> findByTemporadaIdAndCategoria(String temporadaId, CategoriaPremio categoria);

    @Query("""
        SELECT new com.ddo.torneios.dto.PremioTemporadaDTO(
            p.id, p.categoria, p.jogador.id, p.jogadorNomeSnapshot, p.valorEstatistica, p.dataApuracao
        )
        FROM PremioTemporada p
        WHERE p.temporada.id = :temporadaId
        ORDER BY p.categoria
    """)
    List<PremioTemporadaDTO> buscarPremiosDaTemporada(@Param("temporadaId") String temporadaId);

    @Query("""
        SELECT new com.ddo.torneios.dto.PremioTemporadaDTO(
            p.id, p.categoria, p.jogador.id, p.jogadorNomeSnapshot, p.valorEstatistica, p.dataApuracao)
        FROM PremioTemporada p
        WHERE p.jogador.id = :jogadorId
        ORDER BY p.dataApuracao DESC
        """)
    List<PremioTemporadaDTO> buscarPremiosDoJogador(@Param("jogadorId") String jogadorId);

    void deleteByTemporadaId(String temporadaId);
}