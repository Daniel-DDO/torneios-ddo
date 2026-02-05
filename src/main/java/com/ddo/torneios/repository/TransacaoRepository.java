package com.ddo.torneios.repository;

import com.ddo.torneios.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    Page<Transacao> findByJogadorIdOrderByDataHoraDesc(String jogadorId, Pageable pageable);
}