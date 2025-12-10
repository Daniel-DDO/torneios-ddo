package com.ddo.torneios.repository;

import com.ddo.torneios.model.Torneio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TorneioRepository extends JpaRepository<Torneio, String> {
    boolean existsByTemporadaIdAndCompeticaoId(String temporadaId, String competicaoId);
    List<Torneio> findByTemporadaId(String temporadaId);
}