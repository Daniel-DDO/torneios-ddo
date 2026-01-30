package com.ddo.torneios.repository;

import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Lance;
import com.ddo.torneios.model.Leilao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanceRepository extends JpaRepository<Lance, String> {

    Optional<Lance> findByLeilaoAndJogadorAndPrioridade(Leilao leilao, Jogador jogador, Integer prioridade);
    List<Lance> findAllByLeilaoId(String leilaoId);
    List<Lance> findByLeilaoIdAndClubeIdOrderByValorDesc(String leilaoId, String clubeId);
    Optional<Lance> findTopByLeilaoAndClubeOrderByValorDesc(Leilao leilao, Clube clube);
}