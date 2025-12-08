package com.ddo.torneios.repository;

import com.ddo.torneios.model.Temporada;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporadaRepository extends JpaRepository<Temporada, String> {
    boolean existsTemporadaByNome(@NotBlank String nome);
}
