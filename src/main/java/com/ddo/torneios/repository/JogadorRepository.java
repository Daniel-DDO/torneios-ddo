package com.ddo.torneios.repository;

import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.model.Jogador;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, String> {
    boolean existsJogadorByDiscord(@NotBlank String discord);
    Optional<Jogador> findByDiscord(@NotBlank String discord);

    Page<Jogador> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
