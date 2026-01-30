package com.ddo.torneios.repository;

import com.ddo.torneios.model.Anuncio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, String> {
    List<Anuncio> findTop10ByOrderByDataPostagemDesc();
    Optional<Anuncio> findTopByOrderByDataPostagemDesc();
    List<Anuncio> findByTituloContainingIgnoreCase(String titulo);
}