package com.ddo.torneios.repository;

import com.ddo.torneios.dto.AdvertenciaPublicaDTO;
import com.ddo.torneios.dto.ContagemNivelProjecao;
import com.ddo.torneios.model.Advertencia;
import com.ddo.torneios.model.NivelAdvertencia;
import com.ddo.torneios.model.StatusAdvertencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AdvertenciaRepository extends JpaRepository<Advertencia, String> {

    boolean existsByJogadorIdAndNivelAndStatusAndValidaAteAfter(
            String jogadorId, NivelAdvertencia nivel, StatusAdvertencia status, OffsetDateTime agora);

    List<Advertencia> findByJogadorIdAndNivelAndStatusAndValidaAteAfterOrderByOficializadaEmAsc(
            String jogadorId, NivelAdvertencia nivel, StatusAdvertencia status, OffsetDateTime agora);

    List<Advertencia> findBySuspensaoId(String suspensaoId);

    @Query("select a.jogador.id from Advertencia a where a.id = :id")
    Optional<String> buscarJogadorId(@Param("id") String id);

    @Query("select max(a.validaAte) from Advertencia a "
            + "where a.jogador.id = :jogadorId and a.nivel = :nivel and a.status = :status and a.validaAte > :agora")
    Optional<OffsetDateTime> buscarFimDoRegime(
            @Param("jogadorId") String jogadorId,
            @Param("nivel") NivelAdvertencia nivel,
            @Param("status") StatusAdvertencia status,
            @Param("agora") OffsetDateTime agora);

    @Query("select new com.ddo.torneios.dto.ContagemNivelProjecao(a.nivel, count(a)) from Advertencia a "
            + "where a.jogador.id = :jogadorId and a.status = :status and a.validaAte > :agora group by a.nivel")
    List<ContagemNivelProjecao> contarAtivasPorNivel(
            @Param("jogadorId") String jogadorId,
            @Param("status") StatusAdvertencia status,
            @Param("agora") OffsetDateTime agora);

    @Query("select new com.ddo.torneios.dto.AdvertenciaPublicaDTO(a.id, a.oficializadaEm, a.fato, a.artigo, a.nivel, "
            + "a.multaFinal, a.multiplicador, a.fundamentacao, a.status, a.validaAte) from Advertencia a "
            + "where a.jogador.id = :jogadorId and a.status = :status and a.validaAte > :agora "
            + "order by a.oficializadaEm desc")
    List<AdvertenciaPublicaDTO> buscarAtivasPublicas(
            @Param("jogadorId") String jogadorId,
            @Param("status") StatusAdvertencia status,
            @Param("agora") OffsetDateTime agora);

    @Query(value = "select new com.ddo.torneios.dto.AdvertenciaPublicaDTO(a.id, a.oficializadaEm, a.fato, a.artigo, a.nivel, "
            + "a.multaFinal, a.multiplicador, a.fundamentacao, a.status, a.validaAte) from Advertencia a "
            + "where a.jogador.id = :jogadorId order by a.oficializadaEm desc",
            countQuery = "select count(a) from Advertencia a where a.jogador.id = :jogadorId")
    Page<AdvertenciaPublicaDTO> buscarHistoricoPublico(@Param("jogadorId") String jogadorId, Pageable paginacao);

    @Query("select a.id from Advertencia a where a.multaDebitada = false and a.status <> :cancelada "
            + "and a.prazoRecursoAte < :agora and (a.recursoInterposto = false or a.recursoDecididoEm is not null) "
            + "order by a.prazoRecursoAte asc")
    List<String> buscarIdsComMultaPendente(
            @Param("agora") OffsetDateTime agora,
            @Param("cancelada") StatusAdvertencia cancelada,
            Pageable paginacao);
}
