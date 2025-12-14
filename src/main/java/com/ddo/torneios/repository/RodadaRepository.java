package com.ddo.torneios.repository;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Rodada;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RodadaRepository extends JpaRepository<Rodada, String> {
    @Query("SELECT r FROM Rodada r " +
            "LEFT JOIN FETCH r.partidas p " +
            "WHERE r.fase.id = :faseId " +
            "ORDER BY r.numero ASC")
    List<Rodada> buscarTodasPorFaseComPartidas(@Param("faseId") String faseId);

    @Query("SELECT r FROM Rodada r " +
            "WHERE LOWER(r.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
            "OR CAST(r.numero AS string) LIKE :termo")
    List<Rodada> buscarAutocomplete(@Param("termo") String termo, Pageable pageable);

    void deleteByFase(FaseTorneio fase);
}
