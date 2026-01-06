package com.ddo.torneios.repository;

import com.ddo.torneios.model.Titulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TituloRepository extends JpaRepository<Titulo, String> {
    Optional<Titulo> findByNome(String nome);
}