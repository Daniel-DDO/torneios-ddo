package com.ddo.torneios.repository;

import com.ddo.torneios.model.Clube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubeRepository extends JpaRepository<Clube, String> {
    boolean existsBySigla(String sigla);
    boolean existsByNome(String nome);
    List<Clube> findByNomeContainingIgnoreCase(String nome);

    Page<Clube> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
