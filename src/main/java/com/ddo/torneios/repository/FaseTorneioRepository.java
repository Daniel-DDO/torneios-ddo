package com.ddo.torneios.repository;

import com.ddo.torneios.model.FaseTorneio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaseTorneioRepository extends JpaRepository<FaseTorneio, String> {
    List<FaseTorneio> findByTorneioIdOrderByOrdemAsc(String torneioId);
    boolean existsByTorneioIdAndOrdem(String torneioId, Integer ordem);
    List<FaseTorneio> findTop10ByNomeContainingIgnoreCase(String nome);
    Optional<FaseTorneio> findByTorneioIdAndOrdem(String torneioId, Integer ordem);
}