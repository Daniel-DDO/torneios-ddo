package com.ddo.torneios.repository;

import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.LigaClube;
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
    List<Clube> findTop10ByNomeContainingIgnoreCase(String nome);

    List<Clube> findByLigaClube(LigaClube ligaClube);
    List<Clube> findByLigaClubeNot(LigaClube ligaClube);

    Page<Clube> findByLigaClube(LigaClube ligaClube, Pageable pageable);
    Page<Clube> findByLigaClubeNot(LigaClube ligaClube, Pageable pageable);

    Long countByLigaClube(LigaClube liga);

    boolean existsBySiglaIn(List<String> siglas);
    boolean existsByNomeIn(List<String> nomes);
}
