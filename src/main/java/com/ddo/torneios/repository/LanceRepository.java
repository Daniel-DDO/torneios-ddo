package com.ddo.torneios.repository;

import com.ddo.torneios.dto.HistoricoLancesClubeDTO;
import com.ddo.torneios.dto.LanceResumoDTO;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Lance;
import com.ddo.torneios.model.Leilao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanceRepository extends JpaRepository<Lance, String> {

    Optional<Lance> findByLeilaoAndJogadorAndPrioridade(Leilao leilao, Jogador jogador, Integer prioridade);
    List<Lance> findAllByLeilaoId(String leilaoId);
    List<Lance> findByLeilaoIdAndClubeIdOrderByValorDesc(String leilaoId, String clubeId);
    Optional<Lance> findTopByLeilaoAndClubeOrderByValorDesc(Leilao leilao, Clube clube);

    @Query("""
        SELECT new com.ddo.torneios.dto.LanceResumoDTO(
            l.clube.id,
            l.valor,
            l.jogador.nome,
            l.jogador.id
        )
        FROM Lance l
        WHERE l.leilao.id = :leilaoId
        AND l.valor = (
            SELECT MAX(l2.valor) 
            FROM Lance l2 
            WHERE l2.clube.id = l.clube.id 
            AND l2.leilao.id = :leilaoId
        )
    """)
    List<LanceResumoDTO> encontrarMaioresLancesPorLeilao(String leilaoId);

    @Query("""
        SELECT new com.ddo.torneios.dto.HistoricoLancesClubeDTO(
            l.jogador.id,
            l.jogador.nome,
            l.jogador.imagem,
            l.valor,
            l.dataLance
        )
        FROM Lance l
        WHERE l.leilao.id = :leilaoId
        AND l.clube.id = :clubeId
        ORDER BY l.valor DESC
    """)
    List<HistoricoLancesClubeDTO> buscarHistoricoLancesDoClube(String leilaoId, String clubeId);

    List<Lance> findByLeilaoIdAndJogadorIdOrderByPrioridadeAsc(String leilaoId, String jogadorId);

    List<Lance> findByLeilaoAndJogador(Leilao leilao, Jogador jogador);
}