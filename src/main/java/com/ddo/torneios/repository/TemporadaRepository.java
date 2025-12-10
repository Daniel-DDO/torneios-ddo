package com.ddo.torneios.repository;

import com.ddo.torneios.model.Temporada;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemporadaRepository extends JpaRepository<Temporada, String> {
    boolean existsTemporadaByNome(@NotBlank String nome);
    Optional<Temporada> findByNome(String nome);
}
