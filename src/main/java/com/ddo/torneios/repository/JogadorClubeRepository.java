package com.ddo.torneios.repository;

import com.ddo.torneios.model.JogadorClube;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JogadorClubeRepository extends JpaRepository<JogadorClube, String> {
    boolean existsByJogadorIdAndTemporadaId(String jogadorId, String temporadaId);
    boolean existsByClubeIdAndTemporadaId(String clubeId, String temporadaId);
    List<JogadorClube> findByTemporadaId(String temporadaId);
}