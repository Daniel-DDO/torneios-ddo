package com.ddo.torneios.repository;

import com.ddo.torneios.model.Jogador;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, String> {
    boolean findByDiscord(@NotBlank String discord);
}
