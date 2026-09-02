package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Lance;
import com.ddo.torneios.model.Leilao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT new com.ddo.torneios.dto.FeedItemDTO(" +
            "l.jogador.id, " +
            "l.jogador.nome, " +
            "l.clube.id, " +
            "l.clube.nome, " +
            "l.clube.imagem, " +
            "l.valor, " +
            "l.dataHoraLance) " +
            "FROM Lance l " +
            "WHERE l.leilao.id = :leilaoId " +
            "ORDER BY l.dataHoraLance DESC LIMIT 20")
    List<FeedItemDTO> buscarUltimosLances(@Param("leilaoId") String leilaoId);

    @Query("SELECT new com.ddo.torneios.dto.ClubeDisputadoDTO(" +
            "c.id, " +
            "c.nome, " +
            "c.imagem, " +
            "COUNT(l), " +
            "MAX(l.valor)) " +
            "FROM Lance l JOIN l.clube c " +
            "WHERE l.leilao.id = :leilaoId " +
            "GROUP BY c.id, c.nome, c.imagem " +
            "ORDER BY COUNT(l) DESC, MAX(l.valor) DESC")
    List<ClubeDisputadoDTO> buscarClubesMaisDisputados(@Param("leilaoId") String leilaoId, Pageable pageable);

    void deleteByLeilaoIdAndJogadorId(String leilaoId, String jogadorId);

    Optional<Lance> findTopByLeilaoAndClubeOrderByPrioridadeAscValorDesc(Leilao leilao, Clube clube);

    List<Lance> findByLeilaoAndClubeOrderByPrioridadeAscValorDesc(Leilao leilao, Clube clube);

    List<Lance> findByLeilaoOrderByPrioridadeAscValorDesc(Leilao leilao);

    Optional<Lance> findTopByLeilaoAndClubeAndPrioridadeOrderByValorDesc(Leilao leilao, Clube clube, Integer prioridade);

    @Query("SELECT l.id, l.leilao.id, l.prioridade, l.valor FROM Lance l WHERE l.jogador.id = :jogadorId")
    List<Object[]> buscarChavesPorJogador(String jogadorId);

    @Query("SELECT l.id, l.leilao.id, l.prioridade, l.valor FROM Lance l WHERE l.jogador.id IN :jogadorIds")
    List<Object[]> buscarChavesPorJogadores(List<String> jogadorIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Lance l SET l.jogador = :principal WHERE l.id IN :ids")
    void reatribuirJogador(List<String> ids, Jogador principal);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Lance l WHERE l.id IN :ids")
    void deletarPorIds(List<String> ids);

    @Query("""
    SELECT new com.ddo.torneios.dto.LanceAlgoritmoDTO(
        l.id, l.jogador.id, l.jogador.nome,
        l.clube.id, l.clube.nome, l.clube.imagem,
        l.valor, l.prioridade, l.dataHoraLance
    )
    FROM Lance l
    WHERE l.leilao.id = :leilaoId
""")
    List<LanceAlgoritmoDTO> buscarParaAlgoritmo(@Param("leilaoId") String leilaoId);

    @Query("""
    SELECT new com.ddo.torneios.dto.LanceResumoDTO(l.clube.id, l.valor, l.jogador.nome, l.jogador.id)
    FROM Lance l
    WHERE l.leilao.id = :leilaoId AND l.clube.id IN :clubeIds
    AND l.valor = (
        SELECT MAX(l2.valor) FROM Lance l2
        WHERE l2.clube.id = l.clube.id AND l2.leilao.id = :leilaoId
    )
""")
    List<LanceResumoDTO> encontrarMaioresLancesPorClubes(@Param("leilaoId") String leilaoId,
                                                         @Param("clubeIds") java.util.Set<String> clubeIds);

    @Query("""
    SELECT new com.ddo.torneios.dto.ItemDisputaDTO(l.jogador.nome, l.valor, l.prioridade, l.dataHoraLance)
    FROM Lance l
    WHERE l.leilao.id = :leilaoId AND l.clube.id = :clubeId
    ORDER BY l.prioridade ASC, l.valor DESC
""")
    List<ItemDisputaDTO> buscarDisputaProjetada(@Param("leilaoId") String leilaoId, @Param("clubeId") String clubeId);

    @Query("SELECT COUNT(l) FROM Lance l WHERE l.leilao.id = :leilaoId AND l.clube.id = :clubeId")
    long contarLancesDoClube(@Param("leilaoId") String leilaoId, @Param("clubeId") String clubeId);

    @Query("""
    SELECT new com.ddo.torneios.dto.HistoricoLancesClubeDTO(
        l.jogador.id, l.jogador.nome, l.jogador.imagem, l.valor, l.dataLance
    )
    FROM Lance l
    WHERE l.leilao.id = :leilaoId AND l.clube.id = :clubeId
    ORDER BY l.valor DESC
""")
    org.springframework.data.domain.Page<HistoricoLancesClubeDTO> buscarHistoricoLancesDoClube(
            String leilaoId, String clubeId, org.springframework.data.domain.Pageable pageable);

    @Query("select new com.ddo.torneios.dto.LiderLanceDTO(l.jogador.id, l.jogador.nome, l.valor, l.dataHoraLance) " +
            "from Lance l " +
            "where l.leilao = :leilao and l.clube = :clube and l.prioridade = :prioridade " +
            "order by l.valor desc")
    List<LiderLanceDTO> buscarLiderProjetado(@Param("leilao") Leilao leilao,
                                             @Param("clube") Clube clube,
                                             @Param("prioridade") Integer prioridade,
                                             Pageable pageable);

    @Query("select l from Lance l where l.leilao.id = :leilaoId and l.jogador.id = :jogadorId")
    List<Lance> findByLeilaoIdAndJogadorId(@Param("leilaoId") String leilaoId, @Param("jogadorId") String jogadorId);
}