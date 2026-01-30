package com.ddo.torneios.repository;

import com.ddo.torneios.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransferenciaRepository extends JpaRepository<Transferencia, String> {
    List<Transferencia> findByLeilaoIdOrderByValorPagoDesc(String leilaoId);
}