package com.ddo.torneios.repository;

import com.ddo.torneios.model.Torneio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TorneioRepository extends JpaRepository<Torneio, String> {
}
