package com.ddo.torneios.repository;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.ZonaFase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaseTorneioRepository extends JpaRepository<FaseTorneio, String> {
    List<FaseTorneio> findByTorneioIdOrderByOrdemAsc(String torneioId);
    boolean existsByTorneioIdAndOrdem(String torneioId, Integer ordem);
    List<FaseTorneio> findTop10ByNomeContainingIgnoreCase(String nome);
    Optional<FaseTorneio> findByTorneioIdAndOrdem(String torneioId, Integer ordem);

    @Query("SELECT f FROM FaseTorneio f LEFT JOIN FETCH f.zonas WHERE f.id = :faseId")
    Optional<FaseTorneio> buscarComZonas(@Param("faseId") String faseId);
}