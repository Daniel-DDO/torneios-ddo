package com.ddo.torneios.repository;

import com.ddo.torneios.dto.JogadorResumo;
import com.ddo.torneios.dto.JogadorResumoDTO;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Jogador;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JogadorRepository extends JpaRepository<Jogador, String> {
    boolean existsJogadorByDiscord(@NotBlank String discord);

    boolean existsJogadorByEmail(@NotBlank String novoEmail);

    Optional<Jogador> findByDiscord(@NotBlank String discord);

    Optional<Jogador> findByEmail(@NotBlank String email);

    List<Jogador> findByDiscordContainingIgnoreCase(String discord);

    Page<Jogador> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Jogador> findByCargo(Cargo cargo, Pageable pageable);

    Page<Jogador> findByCargoNot(Cargo cargo, Pageable pageable);

    Long countByContaReivindicadaTrue();

    @Query("SELECT j FROM Jogador j WHERE LOWER(j.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR LOWER(j.discord) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<Jogador> buscarAutocomplete(@Param("termo") String termo, Pageable pageable);

    List<Jogador> findByNomeContainingIgnoreCaseOrDiscordContainingIgnoreCase(String nome, String discord);

    @Query("""
        SELECT new com.ddo.torneios.dto.JogadorResumoDTO(
            j.id,
            j.nome,
            j.discord,
            j.pontosCoeficiente,
            j.imagem
        )
        FROM Jogador j
        ORDER BY j.pontosCoeficiente DESC NULLS LAST
    """)
    List<JogadorResumoDTO> buscarRankingCompleto();

    @Query("""
        SELECT new com.ddo.torneios.dto.JogadorResumoDTO(
            j.id,
            j.nome,
            j.discord,
            j.pontosCoeficiente,
            j.imagem
        )
        FROM Jogador j
        ORDER BY j.pontosCoeficiente DESC NULLS LAST
        LIMIT 10
    """)
    List<JogadorResumoDTO> buscarTop10Ranking();

    List<Jogador> findTop10ByOrderByPontosCoeficienteDescTitulosDescFinaisDescVitoriasDesc();

    Page<Jogador> findAllByOrderBySaldoVirtualDesc(Pageable pageable);

    List<JogadorResumo> findByDiscordContainingIgnoreCaseOrNomeContainingIgnoreCase(
            String discord,
            String nome,
            Pageable pageable
    );

    @Query("""
        SELECT new com.ddo.torneios.dto.JogadorResumoDTO(
            j.id,
            j.nome,
            j.discord,
            j.pontosCoeficiente,
            j.imagem
        )
        FROM Jogador j
        WHERE j.id = :id
    """)
    Optional<JogadorResumoDTO> findResumoById(@Param("id") String id);

    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.FETCH)
    Optional<Jogador> findParaAutenticacaoById(String id);
}
