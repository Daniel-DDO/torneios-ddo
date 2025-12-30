package com.ddo.torneios.repository;

import com.ddo.torneios.model.Insignia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InsigniaRepository extends JpaRepository<Insignia, String> {
    Optional<Insignia> findByNome(String nome);
}