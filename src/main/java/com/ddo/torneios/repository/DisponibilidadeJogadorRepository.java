package com.ddo.torneios.repository;

import com.ddo.torneios.dto.DisponibilidadeAtualProjecaoDTO;
import com.ddo.torneios.dto.DisponibilidadeHistoricoDTO;
import com.ddo.torneios.model.DisponibilidadeJogador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadeJogadorRepository extends JpaRepository<DisponibilidadeJogador, Long> {

    @Query("""
        SELECT new com.ddo.torneios.dto.DisponibilidadeAtualProjecaoDTO(d.jogadorId, d.mascara, d.criadoEm)
        FROM DisponibilidadeJogador d
        WHERE d.jogadorId = :jogadorId
        ORDER BY d.criadoEm DESC, d.id DESC
    """)
    List<DisponibilidadeAtualProjecaoDTO> buscarUltimas(@Param("jogadorId") String jogadorId, Pageable pageable);

    /** Versão atual = a mais recente. Uma linha só, pelo índice (jogador_id, criado_em). */
    default Optional<DisponibilidadeAtualProjecaoDTO> buscarAtual(String jogadorId) {
        return buscarUltimas(jogadorId, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query(value = """
        SELECT new com.ddo.torneios.dto.DisponibilidadeHistoricoDTO(d.id, d.mascara, d.criadoEm)
        FROM DisponibilidadeJogador d
        WHERE d.jogadorId = :jogadorId
        ORDER BY d.criadoEm DESC, d.id DESC
    """, countQuery = """
        SELECT COUNT(d) FROM DisponibilidadeJogador d WHERE d.jogadorId = :jogadorId
    """)
    Page<DisponibilidadeHistoricoDTO> listarHistorico(@Param("jogadorId") String jogadorId, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
        FROM DisponibilidadeJogador d
        WHERE d.jogadorId = :jogadorId
    """)
    boolean jogadorTemGrade(@Param("jogadorId") String jogadorId);

    /**
     * Trava de transação (PostgreSQL) por jogador: serializa alterações simultâneas
     * (duplo clique, duas abas) pra regra das 24h não ter brecha. Liberada no fim da transação.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(7001, hashtext(:jogadorId))) AS trava", nativeQuery = true)
    Integer travarAlteracaoDoJogador(@Param("jogadorId") String jogadorId);

    // ------------------------- mesclagem de contas -------------------------

    @Query("""
        SELECT d.id FROM DisponibilidadeJogador d
        WHERE d.jogadorId IN :jogadorIds
        ORDER BY d.criadoEm DESC, d.id DESC
    """)
    List<Long> buscarIdsMaisRecentes(@Param("jogadorIds") Collection<String> jogadorIds, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DisponibilidadeJogador d SET d.jogadorId = :destinoId WHERE d.id = :id")
    int transferirParaJogador(@Param("id") Long id, @Param("destinoId") String destinoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DisponibilidadeJogador d WHERE d.jogadorId IN :jogadorIds")
    int excluirPorJogadores(@Param("jogadorIds") Collection<String> jogadorIds);
}
