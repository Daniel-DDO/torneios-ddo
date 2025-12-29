package com.ddo.torneios.repository;

import com.ddo.torneios.model.ReportPartida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportPartidaRepository extends JpaRepository<ReportPartida, String> {
    Optional<ReportPartida> findByPartida_Id(String partidaId);
}
