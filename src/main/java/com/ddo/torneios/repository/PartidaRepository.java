package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.FaseMataMata;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.TipoPartida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    @Query(value = """
    SELECT new com.ddo.torneios.dto.PartidaHistoricoDTO(
        p.id,
        f.id,
        r.id,
        r.numero,
        p.dataHora,
        p.estadio,
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            m.id, mj.id, mj.nome, mj.imagem, mc.id, mc.nome, mc.imagem, mc.sigla
        ),
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            v.id, vj.id, vj.nome, vj.imagem, vc.id, vc.nome, vc.imagem, vc.sigla
        ),
        p.golsMandante,
        p.golsVisitante,
        p.realizada,
        p.wo,
        CASE WHEN p.penaltis.golsMandante IS NOT NULL AND p.penaltis.golsVisitante IS NOT NULL
             THEN true ELSE false END,
        p.penaltis.golsMandante,
        p.penaltis.golsVisitante
    )
    FROM Partida p
    JOIN p.fase f
    LEFT JOIN p.rodada r
    JOIN p.mandante m JOIN m.jogador mj JOIN m.clube mc
    JOIN p.visitante v JOIN v.jogador vj JOIN v.clube vc
    WHERE (mj.id = :jogadorId OR vj.id = :jogadorId)
    AND p.realizada = :realizada
    """,
            countQuery = """
    SELECT count(p)
    FROM Partida p
    JOIN p.mandante m JOIN m.jogador mj
    JOIN p.visitante v JOIN v.jogador vj
    WHERE (mj.id = :jogadorId OR vj.id = :jogadorId)
    AND p.realizada = :realizada
    """)
    Page<PartidaHistoricoDTO> findPorJogadorIdEStatus(
            @Param("jogadorId") String jogadorId,
            @Param("realizada") boolean realizada,
            Pageable pageable
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

    @Query("SELECT p FROM Partida p WHERE (p.mandante.id = :jcId OR p.visitante.id = :jcId) AND p.realizada = false")
    List<Partida> findPartidasNaoRealizadasPorJogadorClube(@Param("jcId") String jogadorClubeId);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.torneio.id = :torneioId " +
            "AND (p.mandante.jogador.id = :jogadorId OR p.visitante.jogador.id = :jogadorId)")
    List<Partida> findByJogadorAndTorneio(@Param("jogadorId") String jogadorId,
                                          @Param("torneioId") String torneioId,
                                          Sort sort);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.id = :faseId " +
            "AND (p.mandante.jogador.id = :jogadorId OR p.visitante.jogador.id = :jogadorId)")
    List<Partida> findByJogadorAndFase(@Param("jogadorId") String jogadorId,
                                       @Param("faseId") String faseId,
                                       Sort sort);

    @Query("SELECT p FROM Partida p WHERE p.fase.id = :faseId " +
            "AND p.chaveIndex = :chaveIndex " +
            "AND p.tipoPartida = :tipoIda")
    Optional<Partida> findPartidaIda(
            @Param("faseId") String faseId,
            @Param("chaveIndex") Integer chaveIndex,
            @Param("tipoIda") TipoPartida tipoIda
    );

    @Query(value = """
        SELECT 
            COUNT(*) as totalJogos,
            SUM(CASE 
                WHEN (m.jogador_id = :idMandanteAtual AND (p.gols_mandante > p.gols_visitante OR (p.gols_mandante = p.gols_visitante AND p.penaltis_mandante > p.penaltis_visitante))) THEN 1
                WHEN (v.jogador_id = :idMandanteAtual AND (p.gols_visitante > p.gols_mandante OR (p.gols_visitante = p.gols_mandante AND p.penaltis_visitante > p.penaltis_mandante))) THEN 1
                ELSE 0 
            END) as vitoriasMandanteAtual,
            
            SUM(CASE 
                WHEN (p.gols_mandante = p.gols_visitante AND p.penaltis_mandante IS NULL) THEN 1 
                ELSE 0 
            END) as empates,
            
            SUM(CASE 
                WHEN (m.jogador_id = :idVisitanteAtual AND (p.gols_mandante > p.gols_visitante OR (p.gols_mandante = p.gols_visitante AND p.penaltis_mandante > p.penaltis_visitante))) THEN 1
                WHEN (v.jogador_id = :idVisitanteAtual AND (p.gols_visitante > p.gols_mandante OR (p.gols_visitante = p.gols_mandante AND p.penaltis_visitante > p.penaltis_mandante))) THEN 1
                ELSE 0 
            END) as vitoriasVisitanteAtual
            
        FROM partida p
        JOIN jogador_clube m ON p.mandante_id = m.id
        JOIN jogador_clube v ON p.visitante_id = v.id
        WHERE p.realizada = true
        AND (
            (m.jogador_id = :idMandanteAtual AND v.jogador_id = :idVisitanteAtual) 
            OR 
            (m.jogador_id = :idVisitanteAtual AND v.jogador_id = :idMandanteAtual)
        )
    """, nativeQuery = true)
    HistoricoConfrontoProjection findResumoConfrontoDireto(
            @Param("idMandanteAtual") String idMandanteAtual,
            @Param("idVisitanteAtual") String idVisitanteAtual
    );

    @Query("""
    SELECT new com.ddo.torneios.dto.PartidaDetalheProjection(
        p.id, f.id, r.id, r.numero,
        p.etapaMataMata, p.chaveIndex,
        p.dataHora, p.estadio, p.linkPartida,
        new com.ddo.torneios.dto.JogadorClubeDTO(
            m.id, mj.id, mj.nome, mj.imagem,
            mc.id, mc.nome, mc.imagem, mc.sigla,
            mt.id, mt.nome,
            m.totalGolsMarcados, m.totalGolsSofridos, m.partidasJogadas,
            m.pontosCoeficiente, m.statusTemporada,
            m.vitorias, m.empates, m.derrotas,
            m.totalCartoesAmarelos, m.totalCartoesVermelhos, m.balancoFinanceiro
        ),
        new com.ddo.torneios.dto.JogadorClubeDTO(
            v.id, vj.id, vj.nome, vj.imagem,
            vc.id, vc.nome, vc.imagem, vc.sigla,
            vt.id, vt.nome,
            v.totalGolsMarcados, v.totalGolsSofridos, v.partidasJogadas,
            v.pontosCoeficiente, v.statusTemporada,
            v.vitorias, v.empates, v.derrotas,
            v.totalCartoesAmarelos, v.totalCartoesVermelhos, v.balancoFinanceiro
        ),
        p.golsMandante, p.golsVisitante,
        p.realizada, p.wo, p.houveProrrogacao,
        p.penaltis.golsMandante, p.penaltis.golsVisitante,
        p.logEventos,
        p.cartoesAmarelosMandante, p.cartoesVermelhosMandante,
        p.cartoesAmarelosVisitante, p.cartoesVermelhosVisitante,
        p.coeficienteMandante, p.coeficienteVisitante,
        p.tipoPartida,
        pp.id, p.slotNaProxima,
        p.receitaMandante, p.receitaVisitante
    )
    FROM Partida p
    JOIN p.fase f
    LEFT JOIN p.rodada r
    LEFT JOIN p.mandante m LEFT JOIN m.jogador mj LEFT JOIN m.clube mc LEFT JOIN m.temporada mt
    LEFT JOIN p.visitante v LEFT JOIN v.jogador vj LEFT JOIN v.clube vc LEFT JOIN v.temporada vt
    LEFT JOIN p.proximaPartida pp
    WHERE p.id = :id
    """)
    Optional<PartidaDetalheProjection> buscarDetalhePorId(@Param("id") String id);

    @Query("""
    SELECT new com.ddo.torneios.dto.PartidaClassificacaoProjection(
        p.mandante.id, p.visitante.id, p.golsMandante, p.golsVisitante,
        p.cartoesAmarelosMandante, p.cartoesVermelhosMandante,
        p.cartoesAmarelosVisitante, p.cartoesVermelhosVisitante
    )
    FROM Partida p
    WHERE p.fase = :fase AND p.realizada = true
    """)
    List<PartidaClassificacaoProjection> buscarDadosClassificacao(@Param("fase") FaseTorneio fase);

    @Query("""
    SELECT new com.ddo.torneios.dto.PartidaBracketProjection(
        p.id, p.etapaMataMata, p.chaveIndex, p.tipoPartida, p.realizada,
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            m.id, mj.id, mj.nome, mj.imagem, mc.id, mc.nome, mc.imagem, mc.sigla
        ),
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            v.id, vj.id, vj.nome, vj.imagem, vc.id, vc.nome, vc.imagem, vc.sigla
        ),
        p.golsMandante, p.golsVisitante,
        p.penaltis.golsMandante, p.penaltis.golsVisitante
    )
    FROM Partida p
    LEFT JOIN p.mandante m LEFT JOIN m.jogador mj LEFT JOIN m.clube mc
    LEFT JOIN p.visitante v LEFT JOIN v.jogador vj LEFT JOIN v.clube vc
    WHERE p.fase = :fase AND p.etapaMataMata IS NOT NULL
    """)
    List<PartidaBracketProjection> buscarBracketPorFase(@Param("fase") FaseTorneio fase);

    @Query("""
    SELECT new com.ddo.torneios.dto.PartidaConfrontoDTO(
        p.id, p.tipoPartida, p.realizada, p.dataHora, p.estadio,
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            m.id, mj.id, mj.nome, mj.imagem, mc.id, mc.nome, mc.imagem, mc.sigla
        ),
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            v.id, vj.id, vj.nome, vj.imagem, vc.id, vc.nome, vc.imagem, vc.sigla
        ),
        p.golsMandante, p.golsVisitante,
        p.penaltis.golsMandante, p.penaltis.golsVisitante,
        CASE WHEN p.penaltis.golsMandante IS NOT NULL AND p.penaltis.golsVisitante IS NOT NULL
             THEN true ELSE false END
    )
    FROM Partida p
    LEFT JOIN p.mandante m LEFT JOIN m.jogador mj LEFT JOIN m.clube mc
    LEFT JOIN p.visitante v LEFT JOIN v.jogador vj LEFT JOIN v.clube vc
    WHERE p.fase = :fase AND p.etapaMataMata = :etapa AND p.chaveIndex = :chaveIndex
    ORDER BY p.tipoPartida ASC
    """)
    List<PartidaConfrontoDTO> buscarDetalhesConfronto(@Param("fase") FaseTorneio fase,
                                                      @Param("etapa") FaseMataMata etapa,
                                                      @Param("chaveIndex") Integer chaveIndex);

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
    ORDER BY (SUM(t.jogos) - SUM(t.vitoria) - SUM(t.empate)) DESC,
             (SUM(t.golsSofridos) - SUM(t.golsFeitos)) DESC
    LIMIT 3
""", nativeQuery = true)
    List<PatoProjection> findTop3Carrascos(@Param("jogadorId") String jogadorId);

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordePartidaDTO(
        p.id,
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            m.id, mj.id, mj.nome, mj.imagem, mc.id, mc.nome, mc.imagem, mc.sigla
        ),
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            v.id, vj.id, vj.nome, vj.imagem, vc.id, vc.nome, vc.imagem, vc.sigla
        ),
        p.golsMandante, p.golsVisitante, p.dataHora, p.estadio
    )
    FROM Partida p
    LEFT JOIN p.mandante m LEFT JOIN m.jogador mj LEFT JOIN m.clube mc
    LEFT JOIN p.visitante v LEFT JOIN v.jogador vj LEFT JOIN v.clube vc
    WHERE p.realizada = true
    AND (p.golsMandante + p.golsVisitante) = (
        SELECT MAX(p2.golsMandante + p2.golsVisitante) FROM Partida p2 WHERE p2.realizada = true
    )
""")
    List<RecordePartidaDTO> findPartidaComMaisGols();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordePartidaDTO(
        p.id,
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            m.id, mj.id, mj.nome, mj.imagem, mc.id, mc.nome, mc.imagem, mc.sigla
        ),
        new com.ddo.torneios.dto.JogadorClubeResumoDTO(
            v.id, vj.id, vj.nome, vj.imagem, vc.id, vc.nome, vc.imagem, vc.sigla
        ),
        p.golsMandante, p.golsVisitante, p.dataHora, p.estadio
    )
    FROM Partida p
    LEFT JOIN p.mandante m LEFT JOIN m.jogador mj LEFT JOIN m.clube mc
    LEFT JOIN p.visitante v LEFT JOIN v.jogador vj LEFT JOIN v.clube vc
    WHERE p.realizada = true
    AND ABS(p.golsMandante - p.golsVisitante) = (
        SELECT MAX(ABS(p2.golsMandante - p2.golsVisitante)) FROM Partida p2 WHERE p2.realizada = true
    )
""")
    List<RecordePartidaDTO> findMaiorGoleada();

    @Query("SELECT p FROM Partida p " +
            "WHERE p.fase.torneio.id = :torneioId " +
            "AND (p.mandante.id = :jcId OR p.visitante.id = :jcId) " +
            "AND p.realizada = false")
    List<Partida> findPartidasNaoRealizadasPorJogadorClubeETorneio(@Param("jcId") String jogadorClubeId,
                                                                   @Param("torneioId") String torneioId);

    @Query("SELECT COUNT(p) > 0 FROM Partida p WHERE (p.mandante.id = :jcId OR p.visitante.id = :jcId) AND p.realizada = true")
    boolean existsPartidaRealizadaPorJogadorClube(@Param("jcId") String jogadorClubeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Partida p SET p.mandante.id = :sobreviventeId WHERE p.mandante.id = :antigoId")
    void reatribuirMandante(String antigoId, String sobreviventeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Partida p SET p.visitante.id = :sobreviventeId WHERE p.visitante.id = :antigoId")
    void reatribuirVisitante(String antigoId, String sobreviventeId);

    @Query("""
        SELECT new com.ddo.torneios.dto.PartidaResultadoDTO(
            p.id, p.mandante.jogador.id, p.visitante.jogador.id,
            p.golsMandante, p.golsVisitante,
            p.penaltis.golsMandante, p.penaltis.golsVisitante,
            p.wo, p.dataHora
        )
        FROM Partida p
        WHERE p.realizada = true
        AND (p.mandante.jogador.id = :jogadorId OR p.visitante.jogador.id = :jogadorId)
        ORDER BY p.dataHora ASC
    """)
    List<PartidaResultadoDTO> buscarResultadosDoJogador(@Param("jogadorId") String jogadorId);

    @Modifying
    @Query("UPDATE Partida p SET p.rankSnapshotMandante = :snapshot WHERE p.id = :id")
    void atualizarSnapshotMandante(@Param("id") String id, @Param("snapshot") String snapshot);

    @Modifying
    @Query("UPDATE Partida p SET p.rankSnapshotVisitante = :snapshot WHERE p.id = :id")
    void atualizarSnapshotVisitante(@Param("id") String id, @Param("snapshot") String snapshot);

    @Query("SELECT p.rankSnapshotMandante FROM Partida p WHERE p.id = :id")
    String buscarSnapshotMandante(@Param("id") String id);

    @Query("SELECT p.rankSnapshotVisitante FROM Partida p WHERE p.id = :id")
    String buscarSnapshotVisitante(@Param("id") String id);

    @Modifying
    @Query("UPDATE Partida p SET p.rankSnapshotMandante = NULL, p.rankSnapshotVisitante = NULL")
    void limparTodosSnapshots();

    @Modifying
    @Query("UPDATE Partida p SET p.rankSnapshotMandante = NULL WHERE p.mandante.jogador.id = :jogadorId")
    void limparSnapshotsMandanteDoJogador(@Param("jogadorId") String jogadorId);

    @Modifying
    @Query("UPDATE Partida p SET p.rankSnapshotVisitante = NULL WHERE p.visitante.jogador.id = :jogadorId")
    void limparSnapshotsVisitanteDoJogador(@Param("jogadorId") String jogadorId);

    @Query("""
    SELECT new com.ddo.torneios.dto.PlacarIdaDTO(p.golsMandante, p.golsVisitante, p.realizada)
    FROM Partida p
    WHERE p.fase.id = :faseId
      AND p.chaveIndex = :chaveIndex
      AND p.tipoPartida = :tipoIda
      AND p.mandante.id = :visitanteVoltaId
      AND p.visitante.id = :mandanteVoltaId
    ORDER BY p.dataHora DESC
    """)
    List<PlacarIdaDTO> buscarPlacarIda(
            @Param("faseId") String faseId,
            @Param("chaveIndex") Integer chaveIndex,
            @Param("tipoIda") TipoPartida tipoIda,
            @Param("visitanteVoltaId") String visitanteVoltaId,
            @Param("mandanteVoltaId") String mandanteVoltaId);

}
