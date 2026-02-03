package com.ddo.torneios.repository;

import com.ddo.torneios.dto.PatoProjection;
import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Partida;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, String> {
    List<Partida> findByFaseIdOrderByDataHoraAsc(String faseId);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.id = :faseId " +
            "AND (p.mandante.id = :jogadorClubeId OR p.visitante.id = :jogadorClubeId) " +
            "ORDER BY p.dataHora ASC")
    List<Partida> findByJogadorNaFase(@Param("faseId") String faseId,
                                      @Param("jogadorClubeId") String jogadorClubeId);

    @Query("SELECT p FROM Partida p " +
            "WHERE LOWER(p.mandante.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.visitante.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.mandante.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.visitante.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Partida> buscarAutocomplete(@Param("termo") String termo, Pageable pageable);

    List<Partida> findByFaseId(String faseId);
    List<Partida> findByRodadaId(String rodadaId);

    boolean existsByFaseAndRealizadaTrue(FaseTorneio fase);

    @Modifying
    @Query("DELETE FROM Partida p WHERE p.fase = :fase AND p.rodada IS NULL")
    void deleteByFaseAndRodadaIsNull(@Param("fase") FaseTorneio fase);

    List<Partida> findByFaseAndRealizadaTrue(FaseTorneio fase);

    @Query("SELECT COUNT(p) > 0 FROM Partida p " +
            "WHERE p.fase = :fase " +
            "AND p.etapaMataMata = :etapa " +
            "AND p.chaveIndex = :chave " +
            "AND p.realizada = false")
    boolean existeJogoPendente(@Param("fase") FaseTorneio fase,
                               @Param("etapa") FaseMataMata etapa,
                               @Param("chave") Integer chave);

    List<Partida> findByFaseAndEtapaMataMataAndChaveIndex(FaseTorneio fase, FaseMataMata etapa, Integer chave);

    List<Partida> findByFase(FaseTorneio fase);

    @Query("SELECT p FROM Partida p " +
            "WHERE (p.mandante.jogador.id = :jogadorId OR p.visitante.jogador.id = :jogadorId) " +
            "AND p.realizada = :realizada")
    List<Partida> findPorJogadorIdEStatus(
            @Param("jogadorId") String jogadorId,
            @Param("realizada") boolean realizada,
            Sort sort
    );

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.id = :faseId " +
            "AND (p.mandante.jogador.id = :jogadorId OR p.visitante.jogador.id = :jogadorId) " +
            "ORDER BY p.dataHora DESC")
    List<Partida> findPorFaseEJogador(@Param("faseId") String faseId,
                                      @Param("jogadorId") String jogadorId);

    int countByFaseId(String id);

    boolean existsByFaseId(String faseId);

    @Query("SELECT p FROM Partida p WHERE p.fase.id = :faseId ORDER BY p.chaveIndex ASC")
    List<Partida> findPartidasMataMataOrdenadas(@Param("faseId") String faseId);

    @Query("SELECT p FROM Partida p " +
            "JOIN FETCH p.mandante m JOIN FETCH m.jogador jm " +
            "JOIN FETCH p.visitante v JOIN FETCH v.jogador jv " +
            "WHERE p.realizada = true " +
            "AND (jm.id = :jogadorId OR jv.id = :jogadorId)")
    List<Partida> findHistoricoCompletoPorJogador(@Param("jogadorId") String jogadorId);

    @Query(value = """
        SELECT
            t.adversarioId,
            t.adversarioNome,
            t.adversarioDiscord,
            t.adversarioImagem,
            SUM(t.jogos) as totalJogos,
            SUM(t.vitoria) as minhasVitorias,
            SUM(t.empate) as meusEmpates,
            SUM(t.golsFeitos) as meusGols,
            SUM(t.golsSofridos) as golsSofridos
        FROM (
            SELECT
                jv.id as adversarioId,
                jv.nome as adversarioNome,
                jv.discord as adversarioDiscord,
                jv.imagem as adversarioImagem,
                1 as jogos,
                CASE 
                    WHEN p.gols_mandante > p.gols_visitante THEN 1
                    WHEN p.gols_mandante = p.gols_visitante AND COALESCE(p.penaltis_mandante, 0) > COALESCE(p.penaltis_visitante, 0) THEN 1
                    ELSE 0 
                END as vitoria,
                CASE 
                    WHEN p.gols_mandante = p.gols_visitante AND p.penaltis_mandante IS NULL THEN 1
                    ELSE 0 
                END as empate,
                p.gols_mandante as golsFeitos,
                p.gols_visitante as golsSofridos
            FROM partida p
            INNER JOIN jogador_clube m ON p.mandante_id = m.id
            INNER JOIN jogador jm ON m.jogador_id = jm.id
            INNER JOIN jogador_clube v ON p.visitante_id = v.id
            INNER JOIN jogador jv ON v.jogador_id = jv.id
            WHERE jm.id = :jogadorId AND p.realizada = true

            UNION ALL

            SELECT
                jm.id as adversarioId,
                jm.nome as adversarioNome,
                jm.discord as adversarioDiscord,
                jm.imagem as adversarioImagem,
                1 as jogos,
                CASE 
                    WHEN p.gols_visitante > p.gols_mandante THEN 1
                    WHEN p.gols_visitante = p.gols_mandante AND COALESCE(p.penaltis_visitante, 0) > COALESCE(p.penaltis_mandante, 0) THEN 1
                    ELSE 0 
                END as vitoria,
                CASE 
                    WHEN p.gols_visitante = p.gols_mandante AND p.penaltis_mandante IS NULL THEN 1
                    ELSE 0 
                END as empate,
                p.gols_visitante as golsFeitos,
                p.gols_mandante as golsSofridos
            FROM partida p
            INNER JOIN jogador_clube m ON p.mandante_id = m.id
            INNER JOIN jogador jm ON m.jogador_id = jm.id
            INNER JOIN jogador_clube v ON p.visitante_id = v.id
            INNER JOIN jogador jv ON v.jogador_id = jv.id
            WHERE jv.id = :jogadorId AND p.realizada = true
        ) as t
        GROUP BY t.adversarioId, t.adversarioNome, t.adversarioDiscord, t.adversarioImagem
        ORDER BY SUM(t.vitoria) DESC, (SUM(t.golsFeitos) - SUM(t.golsSofridos)) DESC
        LIMIT 3
    """, nativeQuery = true)
    List<PatoProjection> findTop3Patos(@Param("jogadorId") String jogadorId);

    @Query(value = """
        SELECT t.resultado 
        FROM (
            SELECT 
                p.data_hora,
                CASE 
                    WHEN (jm.id = :jogadorId AND (p.gols_mandante > p.gols_visitante OR (p.gols_mandante = p.gols_visitante AND COALESCE(p.penaltis_mandante, 0) > COALESCE(p.penaltis_visitante, 0)))) THEN 'V'
                    WHEN (jv.id = :jogadorId AND (p.gols_visitante > p.gols_mandante OR (p.gols_visitante = p.gols_mandante AND COALESCE(p.penaltis_visitante, 0) > COALESCE(p.penaltis_mandante, 0)))) THEN 'V'
                    WHEN (p.gols_mandante = p.gols_visitante AND p.penaltis_mandante IS NULL) THEN 'E'
                    ELSE 'D'
                END as resultado
            FROM partida p
            INNER JOIN jogador_clube m ON p.mandante_id = m.id
            INNER JOIN jogador jm ON m.jogador_id = jm.id
            INNER JOIN jogador_clube v ON p.visitante_id = v.id
            INNER JOIN jogador jv ON v.jogador_id = jv.id
            WHERE (jm.id = :jogadorId OR jv.id = :jogadorId)
              AND p.realizada = true
            ORDER BY p.data_hora DESC
            LIMIT 5
        ) as t
        ORDER BY t.data_hora ASC
    """, nativeQuery = true)
    List<String> buscarUltimos5Resultados(@Param("jogadorId") String jogadorId);
}
