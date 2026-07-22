package com.ddo.torneios.repository;

import com.ddo.torneios.dto.AproveitamentoProjection;
import com.ddo.torneios.dto.*;
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

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeJogadorDTO(
        j.id, j.nome, j.imagem, CONCAT(j.golsMarcados, ' gols'), j.golsMarcados
    )
    FROM Jogador j
    WHERE j.golsMarcados = (SELECT MAX(j2.golsMarcados) FROM Jogador j2)
""")
    List<RecordeJogadorDTO> findArtilheiroMaximo();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeJogadorDTO(
        j.id, j.nome, j.imagem, CONCAT(j.titulos, ' títulos'), j.titulos
    )
    FROM Jogador j
    WHERE j.titulos = (SELECT MAX(j2.titulos) FROM Jogador j2)
""")
    List<RecordeJogadorDTO> findMaisTitulos();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeJogadorDTO(
        j.id, j.nome, j.imagem, CONCAT(j.finais, ' finais'), j.finais
    )
    FROM Jogador j
    WHERE j.finais = (SELECT MAX(j2.finais) FROM Jogador j2)
""")
    List<RecordeJogadorDTO> findMaisFinais();

    @Query("""
    SELECT new com.ddo.torneios.dto.RecordeJogadorDTO(
        j.id, j.nome, j.imagem, CONCAT(j.partidasJogadas, ' partidas'), j.partidasJogadas
    )
    FROM Jogador j
    WHERE j.partidasJogadas = (SELECT MAX(j2.partidasJogadas) FROM Jogador j2)
""")
    List<RecordeJogadorDTO> findMaisPartidas();

    @Query(value = """
    SELECT
        j.id as jogadorId,
        j.nome as jogadorNome,
        j.imagem as jogadorImagem,
        ROUND(((j.vitorias * 3.0 + j.empates) / (j.partidas_jogadas * 3.0)) * 100, 1) as aproveitamento,
        j.partidas_jogadas as partidasJogadas
    FROM jogador j
    WHERE j.partidas_jogadas >= :minimoPartidas
    ORDER BY aproveitamento DESC
    LIMIT 1
""", nativeQuery = true)
    Optional<AproveitamentoProjection> findMelhorAproveitamento(@Param("minimoPartidas") int minimoPartidas);
}
