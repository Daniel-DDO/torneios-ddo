package com.ddo.torneios.repository;

import com.ddo.torneios.dto.TituloCampeaoDTO;
import com.ddo.torneios.model.Conquista;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}