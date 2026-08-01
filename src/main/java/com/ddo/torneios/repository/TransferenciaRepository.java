package com.ddo.torneios.repository;

import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Transferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransferenciaRepository extends JpaRepository<Transferencia, String> {
    List<Transferencia> findByLeilaoIdOrderByValorPagoDesc(String leilaoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transferencia t SET t.jogador = :principal WHERE t.jogador.id IN :idsAntigos")
    void reatribuirJogador(List<String> idsAntigos, Jogador principal);
}