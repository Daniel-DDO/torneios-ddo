package com.ddo.torneios.repository;

import com.ddo.torneios.dto.ObservacaoDisponibilidadeDTO;
import com.ddo.torneios.model.ObservacaoDisponibilidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ObservacaoDisponibilidadeRepository extends JpaRepository<ObservacaoDisponibilidade, Long> {

    @Query(value = """
        SELECT new com.ddo.torneios.dto.ObservacaoDisponibilidadeDTO(o.id, o.texto, o.criadoEm)
        FROM ObservacaoDisponibilidade o
        WHERE o.jogadorId = :jogadorId
        ORDER BY o.criadoEm DESC, o.id DESC
    """, countQuery = """
        SELECT COUNT(o) FROM ObservacaoDisponibilidade o WHERE o.jogadorId = :jogadorId
    """)
    Page<ObservacaoDisponibilidadeDTO> listarPorJogador(@Param("jogadorId") String jogadorId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ObservacaoDisponibilidade o SET o.jogadorId = :destinoId WHERE o.jogadorId IN :origemIds")
    int transferirParaJogador(@Param("origemIds") Collection<String> origemIds, @Param("destinoId") String destinoId);
}
