package com.ddo.torneios.repository;

import com.ddo.torneios.model.Competicao;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompeticaoRepository extends JpaRepository<Competicao, String> {
    boolean existsByNome(@NotBlank String nome);
}
