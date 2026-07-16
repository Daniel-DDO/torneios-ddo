package com.ddo.torneios.repository;

import com.ddo.torneios.dto.TorneioDTO;
import com.ddo.torneios.model.Torneio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TorneioRepository extends JpaRepository<Torneio, String> {
    boolean existsByTemporadaIdAndCompeticaoId(String temporadaId, String competicaoId);

    @Query("SELECT t FROM Torneio t WHERE t.temporada.id = :temporadaId ORDER BY t.nome ASC")
    List<Torneio> findByTemporadaId(@Param("temporadaId") String temporadaId);

    @Query("""
    SELECT new com.ddo.torneios.dto.TorneioDTO(
        t.id, t.nome, tp.id, tp.nome, c.id, c.nome
    )
    FROM Torneio t
    JOIN t.temporada tp
    JOIN t.competicao c
    WHERE tp.id = :temporadaId
    ORDER BY t.nome ASC
    """)
    List<TorneioDTO> buscarResumoPorTemporada(@Param("temporadaId") String temporadaId);
}