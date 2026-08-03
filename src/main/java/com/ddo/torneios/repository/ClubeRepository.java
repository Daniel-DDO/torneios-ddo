package com.ddo.torneios.repository;

import com.ddo.torneios.dto.ClubeResumoConcessaoView;
import com.ddo.torneios.dto.RecordeClubeDTO;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.LigaClube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubeRepository extends JpaRepository<Clube, String> {
    boolean existsBySigla(String sigla);
    boolean existsByNome(String nome);
    List<Clube> findByNomeContainingIgnoreCase(String nome);

    Page<Clube> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    List<Clube> findTop10ByNomeContainingIgnoreCase(String nome);

    List<Clube> findByLigaClube(LigaClube ligaClube);
    List<Clube> findByLigaClubeNot(LigaClube ligaClube);

    Page<Clube> findByLigaClube(LigaClube ligaClube, Pageable pageable);
    Page<Clube> findByLigaClubeNot(LigaClube ligaClube, Pageable pageable);

    Long countByLigaClube(LigaClube liga);

    boolean existsBySiglaIn(List<String> siglas);
    boolean existsByNomeIn(List<String> nomes);

    @Query("SELECT c.estadio FROM Clube c WHERE c.estrelas >= 5 AND c.estadio IS NOT NULL")
    List<String> findEstadiosDeClubesTop();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeClubeDTO(c.id, c.nome, c.imagem, c.titulos)
    FROM Clube c
    WHERE c.titulos = (SELECT MAX(c2.titulos) FROM Clube c2)
""")
    List<RecordeClubeDTO> findClubeComMaisTitulos();

    @Query("SELECT c.id as id, c.imagem as imagem FROM Clube c WHERE c.id = :id")
    Optional<ClubeResumoConcessaoView> buscarResumoParaConcessao(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Clube c SET c.titulos = COALESCE(c.titulos, 0) + 1 WHERE c.id = :id")
    void incrementarTitulos(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Clube c SET c.titulos = COALESCE(c.titulos, 0) + :qtd WHERE c.id = :id")
    void incrementarTitulos(@Param("id") String id, @Param("qtd") int qtd);
}
