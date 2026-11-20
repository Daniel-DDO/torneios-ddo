package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Equipe;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.ParticipacaoFase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacaoFaseRepository extends JpaRepository<ParticipacaoFase, String> {
    boolean existsByFaseIdAndJogadorClubeId(String faseId, String jogadorClubeId);
    List<ParticipacaoFase> findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(String faseId);
    List<ParticipacaoFase> findTop10ByJogadorClubeClubeNomeContainingIgnoreCase(String nome);
    Optional<ParticipacaoFase> findByFaseAndJogadorClube(FaseTorneio fase, JogadorClube jogadorClube);
    List<ParticipacaoFase> findByFase(FaseTorneio fase);
    Optional<ParticipacaoFase> findByFaseIdAndJogadorClubeId(String faseId, String jogadorClubeId);

    @Query("SELECT p FROM ParticipacaoFase p " +
            "WHERE LOWER(p.jogadorClube.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.jogadorClube.jogador.discord) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<ParticipacaoFase> buscarTop10PorNomeOuDiscord(@Param("termo") String termo, Pageable pageable);

    List<ParticipacaoFase> findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(String faseId, Pageable pageable);

    List<ParticipacaoFase> findByFaseIdOrderByPosicaoClassificacaoAsc(String faseId, Pageable pageable);

    List<ParticipacaoFase> findByJogadorClube(JogadorClube antigoJC);

    @Query("""
    SELECT new com.ddo.torneios.dto.ParticipacaoClassificacaoProjection(
        pf.id, jc.id, j.nome, c.nome, c.imagem
    )
    FROM ParticipacaoFase pf
    JOIN pf.jogadorClube jc
    JOIN jc.jogador j
    JOIN jc.clube c
    WHERE pf.fase.id = :faseId
    """)
    List<ParticipacaoClassificacaoProjection> buscarDadosClassificacao(@Param("faseId") String faseId);

    List<ParticipacaoFase> findByFaseId(String id);

    List<ParticipacaoFase> findByJogadorClube_Id(String jogadorClubeId);

    Optional<ParticipacaoFase> findByFase_IdAndJogadorClube_Id(String faseId, String jogadorClubeId);

    @Query("""
    SELECT DISTINCT p FROM ParticipacaoFase p
    LEFT JOIN FETCH p.historicoJogadorClubeIds
    WHERE p.fase.id = :faseId
    """)
    List<ParticipacaoFase> findByFaseIdComHistorico(@Param("faseId") String faseId);

    @Query("""
    SELECT new com.ddo.torneios.dto.ParticipacaoFaseDTO(
        pf.id, pf.fase.id, pf.fase.nome,
        pf.jogadorClube.id, pf.jogadorClube.jogador.nome,
        pf.jogadorClube.clube.nome, pf.jogadorClube.clube.sigla, pf.jogadorClube.clube.imagem,
        pf.pontos, pf.partidasJogadas, pf.vitorias, pf.empates, pf.derrotas,
        pf.golsPro, pf.golsContra, pf.saldoGols,
        pf.statusClassificacao, pf.posicaoClassificacao
    )
    FROM ParticipacaoFase pf
    WHERE pf.fase.id = :faseId
""")
    List<ParticipacaoFaseDTO> buscarDTOsPorFase(@Param("faseId") String faseId);

    @Modifying
    @Query("UPDATE ParticipacaoFase p SET p.posicaoClassificacao = :posicao, p.pontos = :pontos WHERE p.id = :id")
    void atualizarPosicaoEPontos(@Param("id") String id, @Param("posicao") Integer posicao, @Param("pontos") Integer pontos);

    public interface ParticipacaoJogadorClubeIdProjection {
        String getId();
        String getJogadorClubeId();
    }

    @Query("SELECT p.id AS id, p.jogadorClube.id AS jogadorClubeId FROM ParticipacaoFase p WHERE p.fase.id = :faseId")
    List<ParticipacaoJogadorClubeIdProjection> buscarIdsJogadorClubePorFase(@Param("faseId") String faseId);

    public interface HistoricoAliasProjection {
        String getParticipacaoFaseId();
        String getJogadorClubeIdAntigo();
    }

    @Query(value = """
    SELECT participacao_fase_id AS participacaoFaseId, jogador_clube_id_antigo AS jogadorClubeIdAntigo
    FROM participacao_fase_historico_jc
    WHERE participacao_fase_id IN (SELECT id FROM participacao_fase WHERE fase_id = :faseId)
    """, nativeQuery = true)
    List<HistoricoAliasProjection> buscarHistoricoPorFase(@Param("faseId") String faseId);

    @Query("""
SELECT new com.ddo.torneios.dto.ClassificacaoEquipeBaseDTO(
    e.id, e.nome,
    COALESCE(SUM(p.pontos), 0L), COALESCE(SUM(p.vitorias), 0L),
    COALESCE(SUM(p.golsPro), 0L), COALESCE(SUM(p.saldoGols), 0L),
    MIN(p.posicaoClassificacao))
FROM ParticipacaoFase p JOIN p.equipe e
WHERE p.fase.id = :faseId
GROUP BY e.id, e.nome
""")
    List<ClassificacaoEquipeBaseDTO> classificacaoEquipesBase(@Param("faseId") String faseId);

    @Query("""
SELECT new com.ddo.torneios.dto.ParticipanteRankDTO(
    p.jogadorClube.id, p.equipe.id, p.posicaoClassificacao)
FROM ParticipacaoFase p
WHERE p.fase.id = :faseId AND p.equipe.id IN :equipeIds
ORDER BY p.posicaoClassificacao ASC
""")
    List<ParticipanteRankDTO> rankingIndividualDasEquipes(
            @Param("faseId") String faseId, @Param("equipeIds") List<String> equipeIds);

    @Query("""
SELECT new com.ddo.torneios.dto.JogadorClubeEquipeDTO(p.jogadorClube.id, p.equipe.id)
FROM ParticipacaoFase p
WHERE p.fase.id = :faseId AND p.equipe IS NOT NULL
""")
    List<JogadorClubeEquipeDTO> equipePorJogadorClube(@Param("faseId") String faseId);

    boolean existsByFaseId(String faseId);

    boolean existsByEquipeIdAndJogadorClubeJogadorId(String equipeId, String jogadorId);

    boolean existsByEquipeId(String equipeId);

    @Query("""
SELECT new com.ddo.torneios.dto.ParticipacaoEquipeDTO(
    p.id, jc.id, e.id, p.fase.id, jc.jogador.id)
FROM ParticipacaoFase p
JOIN p.jogadorClube jc
LEFT JOIN p.equipe e
WHERE p.fase.id = :faseId AND jc.id IN :jcIds
""")
    List<ParticipacaoEquipeDTO> buscarPorFaseEJogadorClubes(@Param("faseId") String faseId, @Param("jcIds") List<String> jcIds);

    @Query("""
SELECT new com.ddo.torneios.dto.ParticipacaoEquipeDTO(
    p.id, p.jogadorClube.id, p.equipe.id, p.fase.id, p.jogadorClube.jogador.id)
FROM ParticipacaoFase p WHERE p.id = :id AND p.equipe IS NOT NULL
""")
    Optional<ParticipacaoEquipeDTO> buscarParticipacaoComEquipe(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ParticipacaoFase p SET p.equipe = :equipe WHERE p.id IN :ids")
    int definirEquipe(@Param("ids") List<String> ids, @Param("equipe") Equipe equipe);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ParticipacaoFase p SET p.equipe = null WHERE p.id = :id")
    int limparEquipe(@Param("id") String id);

    @Query("""
SELECT new com.ddo.torneios.dto.MembroEquipeDTO(
    e.id, p.id, j.id, j.nome, c.nome, CASE WHEN j.id = e.capitao.id THEN true ELSE false END,
    p.pontos, p.partidasJogadas, p.vitorias, p.empates, p.derrotas,
    p.golsPro, p.golsContra, p.saldoGols, p.posicaoClassificacao)
FROM ParticipacaoFase p JOIN p.equipe e JOIN p.jogadorClube jc JOIN jc.jogador j JOIN jc.clube c
WHERE p.fase.id = :faseId
ORDER BY p.posicaoClassificacao ASC NULLS LAST, p.pontos DESC
""")
    List<MembroEquipeDTO> membrosDaFase(@Param("faseId") String faseId);

    @Query("""
SELECT new com.ddo.torneios.dto.MembroEquipeDTO(
    e.id, p.id, j.id, j.nome, c.nome, CASE WHEN j.id = e.capitao.id THEN true ELSE false END,
    p.pontos, p.partidasJogadas, p.vitorias, p.empates, p.derrotas,
    p.golsPro, p.golsContra, p.saldoGols, p.posicaoClassificacao)
FROM ParticipacaoFase p JOIN p.equipe e JOIN p.jogadorClube jc JOIN jc.jogador j JOIN jc.clube c
WHERE p.fase.id = :faseId AND e.id = :equipeId
ORDER BY p.posicaoClassificacao ASC NULLS LAST, p.pontos DESC
""")
    List<MembroEquipeDTO> membrosDaEquipe(@Param("faseId") String faseId, @Param("equipeId") String equipeId);

    @Query("""
SELECT new com.ddo.torneios.dto.EquipeContagemDTO(
    e.id, e.nome, COUNT(p),
    SUM(CASE WHEN p.jogadorClube.jogador.id = e.capitao.id THEN 1L ELSE 0L END))
FROM ParticipacaoFase p JOIN p.equipe e
WHERE p.fase.id = :faseId
GROUP BY e.id, e.nome
""")
    List<EquipeContagemDTO> contagemPorEquipe(@Param("faseId") String faseId);
}