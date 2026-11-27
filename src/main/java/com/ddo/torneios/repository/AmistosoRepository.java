package com.ddo.torneios.repository;

import com.ddo.torneios.dto.AmistosoDTO;
import com.ddo.torneios.dto.AmistosoEstatisticaDTO;
import com.ddo.torneios.model.Amistoso;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AmistosoRepository extends JpaRepository<Amistoso, String> {

    // Lock para impedir duplo pagamento se dois registros/correções chegarem juntos.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Amistoso a WHERE a.id = :id")
    Optional<Amistoso> buscarParaAtualizar(@Param("id") String id);

    @Query("""
    SELECT new com.ddo.torneios.dto.AmistosoDTO(
        a.id,
        new com.ddo.torneios.dto.AmistosoLadoDTO(jm.id, jm.nome, jm.imagem, cm.id, cm.nome, cm.imagem, cm.sigla),
        new com.ddo.torneios.dto.AmistosoLadoDTO(jv.id, jv.nome, jv.imagem, cv.id, cv.nome, cv.imagem, cv.sigla),
        a.dataHora, a.realizada, a.wo, a.golsMandante, a.golsVisitante,
        a.cartoesAmarelosMandante, a.cartoesVermelhosMandante,
        a.cartoesAmarelosVisitante, a.cartoesVermelhosVisitante,
        a.receitaMandante, a.receitaVisitante, a.linkPartida, a.criadoEm, a.registradoEm
    )
    FROM Amistoso a
    JOIN a.jogadorMandante jm JOIN a.clubeMandante cm
    JOIN a.jogadorVisitante jv JOIN a.clubeVisitante cv
    WHERE a.id = :id
    """)
    Optional<AmistosoDTO> buscarDetalhe(@Param("id") String id);

    @Query(value = """
    SELECT new com.ddo.torneios.dto.AmistosoDTO(
        a.id,
        new com.ddo.torneios.dto.AmistosoLadoDTO(jm.id, jm.nome, jm.imagem, cm.id, cm.nome, cm.imagem, cm.sigla),
        new com.ddo.torneios.dto.AmistosoLadoDTO(jv.id, jv.nome, jv.imagem, cv.id, cv.nome, cv.imagem, cv.sigla),
        a.dataHora, a.realizada, a.wo, a.golsMandante, a.golsVisitante,
        a.cartoesAmarelosMandante, a.cartoesVermelhosMandante,
        a.cartoesAmarelosVisitante, a.cartoesVermelhosVisitante,
        a.receitaMandante, a.receitaVisitante, a.linkPartida, a.criadoEm, a.registradoEm
    )
    FROM Amistoso a
    JOIN a.jogadorMandante jm JOIN a.clubeMandante cm
    JOIN a.jogadorVisitante jv JOIN a.clubeVisitante cv
    ORDER BY a.criadoEm DESC
    """,
            countQuery = "SELECT count(a) FROM Amistoso a")
    Page<AmistosoDTO> listarTodos(Pageable pageable);

    @Query(value = """
    SELECT new com.ddo.torneios.dto.AmistosoDTO(
        a.id,
        new com.ddo.torneios.dto.AmistosoLadoDTO(jm.id, jm.nome, jm.imagem, cm.id, cm.nome, cm.imagem, cm.sigla),
        new com.ddo.torneios.dto.AmistosoLadoDTO(jv.id, jv.nome, jv.imagem, cv.id, cv.nome, cv.imagem, cv.sigla),
        a.dataHora, a.realizada, a.wo, a.golsMandante, a.golsVisitante,
        a.cartoesAmarelosMandante, a.cartoesVermelhosMandante,
        a.cartoesAmarelosVisitante, a.cartoesVermelhosVisitante,
        a.receitaMandante, a.receitaVisitante, a.linkPartida, a.criadoEm, a.registradoEm
    )
    FROM Amistoso a
    JOIN a.jogadorMandante jm JOIN a.clubeMandante cm
    JOIN a.jogadorVisitante jv JOIN a.clubeVisitante cv
    WHERE a.realizada = :realizada
    ORDER BY a.criadoEm DESC
    """,
            countQuery = "SELECT count(a) FROM Amistoso a WHERE a.realizada = :realizada")
    Page<AmistosoDTO> listarPorStatus(@Param("realizada") boolean realizada, Pageable pageable);

    @Query(value = """
    SELECT new com.ddo.torneios.dto.AmistosoDTO(
        a.id,
        new com.ddo.torneios.dto.AmistosoLadoDTO(jm.id, jm.nome, jm.imagem, cm.id, cm.nome, cm.imagem, cm.sigla),
        new com.ddo.torneios.dto.AmistosoLadoDTO(jv.id, jv.nome, jv.imagem, cv.id, cv.nome, cv.imagem, cv.sigla),
        a.dataHora, a.realizada, a.wo, a.golsMandante, a.golsVisitante,
        a.cartoesAmarelosMandante, a.cartoesVermelhosMandante,
        a.cartoesAmarelosVisitante, a.cartoesVermelhosVisitante,
        a.receitaMandante, a.receitaVisitante, a.linkPartida, a.criadoEm, a.registradoEm
    )
    FROM Amistoso a
    JOIN a.jogadorMandante jm JOIN a.clubeMandante cm
    JOIN a.jogadorVisitante jv JOIN a.clubeVisitante cv
    WHERE (jm.id = :jogadorId OR jv.id = :jogadorId)
    ORDER BY a.criadoEm DESC
    """,
            countQuery = """
    SELECT count(a) FROM Amistoso a
    WHERE a.jogadorMandante.id = :jogadorId OR a.jogadorVisitante.id = :jogadorId
    """)
    Page<AmistosoDTO> listarPorJogador(@Param("jogadorId") String jogadorId, Pageable pageable);

    @Query("""
    SELECT new com.ddo.torneios.dto.AmistosoEstatisticaDTO(
        COUNT(a),
        COALESCE(SUM(CASE WHEN (a.jogadorMandante.id = :jogadorId AND a.golsMandante > a.golsVisitante)
                            OR (a.jogadorVisitante.id = :jogadorId AND a.golsVisitante > a.golsMandante)
                          THEN 1L ELSE 0L END), 0L),
        COALESCE(SUM(CASE WHEN a.golsMandante = a.golsVisitante THEN 1L ELSE 0L END), 0L),
        COALESCE(SUM(CASE WHEN (a.jogadorMandante.id = :jogadorId AND a.golsMandante < a.golsVisitante)
                            OR (a.jogadorVisitante.id = :jogadorId AND a.golsVisitante < a.golsMandante)
                          THEN 1L ELSE 0L END), 0L),
        COALESCE(SUM(CASE WHEN a.jogadorMandante.id = :jogadorId THEN a.golsMandante ELSE a.golsVisitante END), 0L),
        COALESCE(SUM(CASE WHEN a.jogadorMandante.id = :jogadorId THEN a.golsVisitante ELSE a.golsMandante END), 0L),
        COALESCE(SUM(CASE WHEN a.jogadorMandante.id = :jogadorId THEN a.cartoesAmarelosMandante ELSE a.cartoesAmarelosVisitante END), 0L),
        COALESCE(SUM(CASE WHEN a.jogadorMandante.id = :jogadorId THEN a.cartoesVermelhosMandante ELSE a.cartoesVermelhosVisitante END), 0L)
    )
    FROM Amistoso a
    WHERE a.realizada = true
      AND (a.jogadorMandante.id = :jogadorId OR a.jogadorVisitante.id = :jogadorId)
    """)
    AmistosoEstatisticaDTO buscarEstatisticasDoJogador(@Param("jogadorId") String jogadorId);
}