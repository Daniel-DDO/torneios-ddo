package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Conquista;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConquistaRepository extends JpaRepository<Conquista, String> {
    Optional<Conquista> findFirstByOrderByDataConquistaDesc();
    List<Conquista> findTop10ByOrderByDataConquistaDesc();
    List<Conquista> findByJogadorIdOrderByDataConquistaDesc(String jogadorId);
    List<Conquista> findByClubeIdOrderByDataConquistaDesc(String clubeId);
    List<Conquista> findByTituloIdOrderByDataConquistaDesc(String tituloId);
    List<Conquista> findByDataConquistaBetweenOrderByDataConquistaDesc(LocalDateTime inicio, LocalDateTime fim);
    boolean existsByTituloIdAndNomeEdicaoAndJogadorId(String tituloId, String nomeEdicao, String jogadorId);

    @Query("SELECT new com.ddo.torneios.dto.TituloCampeaoDTO(" +
            "j.id, j.nome, j.imagem, COUNT(c)) " +
            "FROM Conquista c " +
            "JOIN c.jogador j " +
            "WHERE c.titulo.id = :tituloId " +
            "GROUP BY j.id, j.nome, j.imagem " +
            "ORDER BY COUNT(c) DESC")
    List<TituloCampeaoDTO> findTop3CampeoesPorTitulo(@Param("tituloId") String tituloId, Pageable pageable);

    @Query("""
    SELECT new com.ddo.torneios.dto.ConquistaDashboardDTO(
        c.id, c.titulo.id, c.titulo.nome, c.nomeEdicao, c.imagem,
        c.jogador.id, c.jogador.nome, c.jogador.imagem,
        c.clube.id, c.clube.nome, c.clube.sigla, c.clube.imagem,
        c.dataConquista
    )
    FROM Conquista c
    LEFT JOIN c.clube
    ORDER BY c.dataConquista DESC
    """)
    List<ConquistaDashboardDTO> buscarUltimasConquistasDTO(Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Conquista c SET c.imagem = :imagem WHERE c.id = :id")
    void atualizarImagem(@Param("id") String id, @Param("imagem") String imagem);

    @Query("""
        SELECT c.titulo.imagemGerarPost AS tituloImagemGerarPost,
               c.titulo.nome AS tituloNome,
               c.jogador.id AS jogadorId,
               c.jogador.nome AS jogadorNome,
               c.jogador.imagem AS jogadorImagem,
               c.clube.imagem AS clubeImagem
        FROM Conquista c
        WHERE c.id = :conquistaId
        """)
    Optional<ConquistaParaRegeracaoView> buscarParaRegeracaoImagem(@Param("conquistaId") String conquistaId);

    @Query("""
        SELECT NEW com.ddo.torneios.dto.ConquistaResumoDTO(
            c.id, c.titulo.nome, c.titulo.imagem, c.nomeEdicao,
            c.jogador.nome, c.clube.nome, c.imagem, c.dataConquista
        )
        FROM Conquista c
        ORDER BY c.dataConquista DESC
        """)
    List<ConquistaResumoDTO> buscarTodasResumo();

    @Query("""
        SELECT NEW com.ddo.torneios.dto.JogadorDestaqueClubeDTO(
            j.id, j.nome, j.imagem, COUNT(c)
        )
        FROM Conquista c
        JOIN c.jogador j
        WHERE c.clube.id = :clubeId
        GROUP BY j.id, j.nome, j.imagem
        ORDER BY COUNT(c) DESC
        """)
    List<JogadorDestaqueClubeDTO> buscarJogadorDestaquePorClube(String clubeId, Pageable pageable);
}