package com.ddo.torneios.repository;

import com.ddo.torneios.dto.TituloResumoDTO;
import com.ddo.torneios.model.Titulo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TituloRepository extends JpaRepository<Titulo, String> {
    Optional<Titulo> findByNome(String nome);
    List<Titulo> findByAtivoTrue();
    List<Titulo> findByAtivoFalse();
    List<Titulo> findByAtivo(Boolean ativo);

    @Query("""
    SELECT new com.ddo.torneios.dto.TituloResumoDTO(t.id, t.nome, t.imagem)
    FROM Titulo t
    WHERE LOWER(t.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
    ORDER BY t.nome ASC
    """)
    List<TituloResumoDTO> buscarAutocomplete(@Param("termo") String termo, Pageable pageable);
}