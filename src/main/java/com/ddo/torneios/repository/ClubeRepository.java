package com.ddo.torneios.repository;

import com.ddo.torneios.model.Clube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubeRepository extends JpaRepository<Clube, String> {
    boolean existsBySigla(String sigla);
    Page<Clube> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
