package com.ddo.torneios.repository;

import com.ddo.torneios.model.ParticipacaoFase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipacaoFaseRepository extends JpaRepository<ParticipacaoFase, String> {
    boolean existsByFaseIdAndJogadorClubeId(String faseId, String jogadorClubeId);
    List<ParticipacaoFase> findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(String faseId);
}