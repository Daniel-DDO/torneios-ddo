package com.ddo.torneios.repository;

import com.ddo.torneios.dto.CompeticaoDTO;
import com.ddo.torneios.model.Competicao;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompeticaoRepository extends JpaRepository<Competicao, String> {
    boolean existsByNome(@NotBlank String nome);

    Page<Competicao> findByNomeContainingIgnoreCase(String nomeFiltro, Pageable pageable);

    @Query("""
    SELECT new com.ddo.torneios.dto.CompeticaoDTO(
        c.id, c.nome, c.imagem, c.divisao, c.valor, c.descricao,
        t.id, t.nome, t.imagem
    )
    FROM Competicao c
    LEFT JOIN c.titulo t
    WHERE c.id = :id
    """)
    Optional<CompeticaoDTO> buscarDetalhesPorId(@Param("id") String id);
}
