package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.Temporada;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JogadorClubeRepository extends JpaRepository<JogadorClube, String> {
    boolean existsByJogadorIdAndTemporadaId(String jogadorId, String temporadaId);
    boolean existsByClubeIdAndTemporadaId(String clubeId, String temporadaId);
    Optional<JogadorClube> findByJogadorIdAndTemporadaId(String jogadorId, String temporadaId);
    List<JogadorClube> findByTemporadaId(String temporadaId);
    List<JogadorClube> findTop10ByJogadorNomeContainingIgnoreCase(String nome);
    List<JogadorClube> findTop10ByClubeNomeContainingIgnoreCase(String nome);

    @Query("SELECT jc FROM JogadorClube jc " +
            "WHERE jc.temporada.id = :temporadaId " +
            "AND (" +
            "  LOWER(jc.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.jogador.discord) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.clube.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "  LOWER(jc.clube.sigla) LIKE LOWER(CONCAT('%', :termo, '%'))" +
            ")")
    List<JogadorClube> buscarPorTermoETemporada(
            @Param("termo") String termo,
            @Param("temporadaId") String temporadaId,
            Pageable pageable
    );

    List<JogadorClube> findTop6ByTemporadaOrderByPontosCoeficienteDesc(Temporada temporada);

    @Query("SELECT jc.idDeQuemMeSubstituiu FROM JogadorClube jc WHERE jc.id = :id")
    Optional<String> buscarIdSubstituto(@Param("id") String id);

    @Query("""
    SELECT new com.ddo.torneios.dto.JogadorClubeInscritoDTO(
        jc.id, j.nome, c.nome, c.imagem,
        jc.partidasJogadas, jc.vitorias, jc.empates, jc.derrotas,
        jc.totalGolsMarcados, jc.totalGolsSofridos, jc.pontosCoeficiente
    )
    FROM JogadorClube jc
    JOIN jc.jogador j
    JOIN jc.clube c
    WHERE jc.temporada.id = :temporadaId
    ORDER BY jc.pontosCoeficiente DESC
    """)
    List<JogadorClubeInscritoDTO> buscarInscritosResumo(@Param("temporadaId") String temporadaId);

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeTemporadaDTO(
        jc.jogador.id, jc.jogador.nome, jc.jogador.imagem,
        jc.temporada.nome, jc.totalGolsMarcados, jc.partidasJogadas
    )
    FROM JogadorClube jc
    WHERE jc.totalGolsMarcados = (SELECT MAX(jc2.totalGolsMarcados) FROM JogadorClube jc2)
""")
    List<RecordeTemporadaDTO> findMelhorAtaqueTemporada();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeTemporadaDTO(
        jc.jogador.id, jc.jogador.nome, jc.jogador.imagem,
        jc.temporada.nome, jc.totalGolsSofridos, jc.partidasJogadas
    )
    FROM JogadorClube jc
    WHERE jc.partidasJogadas >= :minimoPartidas
    AND jc.totalGolsSofridos = (
        SELECT MIN(jc2.totalGolsSofridos) FROM JogadorClube jc2 WHERE jc2.partidasJogadas >= :minimoPartidas
    )
""")
    List<RecordeTemporadaDTO> findMelhorDefesaTemporada(@Param("minimoPartidas") int minimoPartidas);

    @Query("SELECT jc.id, jc.temporada.id FROM JogadorClube jc WHERE jc.jogador.id = :jogadorId")
    List<Object[]> buscarIdETemporadaPorJogador(String jogadorId);

    @Query("SELECT jc.id, jc.temporada.id FROM JogadorClube jc WHERE jc.jogador.id IN :jogadorIds")
    List<Object[]> buscarIdETemporadaPorJogadores(List<String> jogadorIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JogadorClube jc SET jc.jogador = :principal WHERE jc.id IN :ids")
    void reatribuirJogador(List<String> ids, Jogador principal);

    @Query("""
    SELECT jc.id as jogadorClubeId,
           j.id as jogadorId, j.nome as jogadorNome, j.imagem as jogadorImagem,
           c.id as clubeId, c.imagem as clubeImagem
    FROM JogadorClube jc
    JOIN jc.jogador j
    JOIN jc.clube c
    WHERE jc.id = :id
""")
    Optional<JogadorClubeConcessaoView> buscarParaConcessao(@Param("id") String id);

    @Query("""
    SELECT new com.ddo.torneios.dto.EstatisticaTemporadaDTO(
        jc.id, j.id, j.nome,
        jc.totalGolsMarcados, jc.totalGolsSofridos, jc.partidasJogadas,
        jc.totalCartoesAmarelos, jc.totalCartoesVermelhos, jc.pontosCoeficiente,
        j.rankPoints
    )
    FROM JogadorClube jc
    JOIN jc.jogador j
    WHERE jc.temporada.id = :temporadaId
""")
    List<EstatisticaTemporadaDTO> buscarEstatisticasTemporada(@Param("temporadaId") String temporadaId);

    @Query("""
    SELECT new com.ddo.torneios.dto.MelhorTemporadaDTO(
        t.id, t.nome, t.dataInicio, t.dataFim, t.ativa,
        jc.partidasJogadas, jc.vitorias, jc.empates, jc.derrotas,
        jc.totalGolsMarcados, jc.totalGolsSofridos,
        (jc.totalGolsMarcados - jc.totalGolsSofridos),
        (1.0 * jc.totalGolsMarcados / jc.partidasJogadas),
        (1.0 * jc.totalGolsSofridos / jc.partidasJogadas),
        jc.aproveitamento,
        jc.aproveitamento
            + (1.0 * (jc.totalGolsMarcados - jc.totalGolsSofridos) / jc.partidasJogadas) * 10.0
            + LEAST(jc.partidasJogadas, 20) * 0.5
    )
    FROM JogadorClube jc
    JOIN jc.temporada t
    WHERE jc.jogador.id = :jogadorId
      AND jc.partidasJogadas > 0
    ORDER BY 16 DESC
    """)
    List<MelhorTemporadaDTO> buscarMelhoresTemporadas(@Param("jogadorId") String jogadorId, Pageable pageable);

    @Query("""
    SELECT new com.ddo.torneios.dto.AgregadoEstiloDTO(
        SUM(jc.partidasJogadas),
        SUM(jc.totalGolsMarcados),
        SUM(jc.totalGolsSofridos),
        AVG(c.estrelas)
    )
    FROM JogadorClube jc
    JOIN jc.clube c
    WHERE jc.jogador.id = :jogadorId
      AND jc.partidasJogadas > 0
    """)
    AgregadoEstiloDTO buscarAgregadoEstiloJogador(@Param("jogadorId") String jogadorId);

    @Query("""
    SELECT new com.ddo.torneios.dto.MediasGlobaisEstiloDTO(
        AVG(1.0 * jc.totalGolsMarcados / jc.partidasJogadas),
        AVG(1.0 * jc.totalGolsSofridos / jc.partidasJogadas),
        AVG(c.estrelas)
    )
    FROM JogadorClube jc
    JOIN jc.clube c
    WHERE jc.partidasJogadas > 0
    """)
    MediasGlobaisEstiloDTO buscarMediasGlobaisEstilo();

    @Query("""
    SELECT new com.ddo.torneios.dto.AgregadoEstiloParDTO(
        SUM(CASE WHEN jc.jogador.id = :id1 THEN jc.partidasJogadas ELSE 0 END),
        SUM(CASE WHEN jc.jogador.id = :id1 THEN jc.totalGolsMarcados ELSE 0 END),
        SUM(CASE WHEN jc.jogador.id = :id1 THEN jc.totalGolsSofridos ELSE 0 END),
        AVG(CASE WHEN jc.jogador.id = :id1 THEN c.estrelas ELSE NULL END),

        SUM(CASE WHEN jc.jogador.id = :id2 THEN jc.partidasJogadas ELSE 0 END),
        SUM(CASE WHEN jc.jogador.id = :id2 THEN jc.totalGolsMarcados ELSE 0 END),
        SUM(CASE WHEN jc.jogador.id = :id2 THEN jc.totalGolsSofridos ELSE 0 END),
        AVG(CASE WHEN jc.jogador.id = :id2 THEN c.estrelas ELSE NULL END)
    )
    FROM JogadorClube jc
    JOIN jc.clube c
    WHERE jc.jogador.id IN (:id1, :id2)
      AND jc.partidasJogadas > 0
    """)
    AgregadoEstiloParDTO buscarAgregadoEstiloPar(@Param("id1") String id1, @Param("id2") String id2);

    @Query("""
    SELECT new com.ddo.torneios.dto.JogadorClubeBaseDTO(jc.id, jc.clube.id, jc.temporada.id)
    FROM JogadorClube jc
    WHERE jc.id = :id
    """)
    Optional<JogadorClubeBaseDTO> buscarBasePorId(@Param("id") String id);
}