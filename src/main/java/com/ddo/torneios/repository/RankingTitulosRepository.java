package com.ddo.torneios.repository;

import com.ddo.torneios.dto.JogadorTitulosProjecao;
import com.ddo.torneios.model.Jogador;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RankingTitulosRepository extends JpaRepository<Jogador, String> {

    @Query("select new com.ddo.torneios.dto.JogadorTitulosProjecao(j.id, j.nome, j.titulos, j.finais) "
            + "from Jogador j where j.titulos is not null and j.titulos > 0 "
            + "order by j.titulos desc, j.finais desc, j.nome asc")
    List<JogadorTitulosProjecao> buscarTopTitulos(Pageable paginacao);
}
