package com.ddo.torneios.repository;

import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Page<Transacao> findByJogadorIdOrderByDataHoraDesc(String jogadorId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transacao t SET t.jogador = :principal WHERE t.jogador.id IN :idsAntigos")
    void reatribuirJogador(List<String> idsAntigos, Jogador principal);
}