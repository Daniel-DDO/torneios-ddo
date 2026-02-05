package com.ddo.torneios.repository;

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
}