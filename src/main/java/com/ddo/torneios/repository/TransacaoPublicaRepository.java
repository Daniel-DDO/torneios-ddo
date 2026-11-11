package com.ddo.torneios.repository;

import com.ddo.torneios.dto.TransacaoPublicaDTO;
import com.ddo.torneios.model.Transacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransacaoPublicaRepository extends JpaRepository<Transacao, Long> {

    @Query(value = "select new com.ddo.torneios.dto.TransacaoPublicaDTO(t.id, t.jogador.id, t.jogador.nome, "
            + "t.tipo, t.valor, t.motivo, t.dataHora) from Transacao t order by t.dataHora desc, t.id desc",
            countQuery = "select count(t) from Transacao t")
    Page<TransacaoPublicaDTO> buscarUltimasTransacoes(Pageable paginacao);
}
