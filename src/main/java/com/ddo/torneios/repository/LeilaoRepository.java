package com.ddo.torneios.repository;

import com.ddo.torneios.model.Leilao;
import com.ddo.torneios.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeilaoRepository extends JpaRepository<Leilao, String> {
    boolean existsByTemporadaAndAtivoTrue(Temporada temporada);
}
