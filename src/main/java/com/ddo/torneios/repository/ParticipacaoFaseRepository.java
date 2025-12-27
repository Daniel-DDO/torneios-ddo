package com.ddo.torneios.repository;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.ParticipacaoFase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacaoFaseRepository extends JpaRepository<ParticipacaoFase, String> {
    boolean existsByFaseIdAndJogadorClubeId(String faseId, String jogadorClubeId);
    List<ParticipacaoFase> findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(String faseId);
    List<ParticipacaoFase> findTop10ByJogadorClubeClubeNomeContainingIgnoreCase(String nome);
    Optional<ParticipacaoFase> findByFaseAndJogadorClube(FaseTorneio fase, JogadorClube jogadorClube);
    List<ParticipacaoFase> findByFase(FaseTorneio fase);
    Optional<ParticipacaoFase> findByFaseIdAndJogadorClubeId(String faseId, String jogadorClubeId);

    @Query("SELECT p FROM ParticipacaoFase p " +
            "WHERE LOWER(p.jogadorClube.jogador.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR LOWER(p.jogadorClube.jogador.discord) LIKE LOWER(CONCAT('%', :termo, '%'))")
    List<ParticipacaoFase> buscarTop10PorNomeOuDiscord(@Param("termo") String termo, Pageable pageable);
}