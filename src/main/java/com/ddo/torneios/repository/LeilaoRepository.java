package com.ddo.torneios.repository;

import com.ddo.torneios.dto.LeilaoResumoDTO;
import com.ddo.torneios.model.Leilao;
import com.ddo.torneios.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LeilaoRepository extends JpaRepository<Leilao, String> {
    boolean existsByTemporadaAndAtivoTrue(Temporada temporada);
    List<Leilao> findByTemporadaIdOrderByDataInicioDesc(String temporadaId);
    boolean existsByTemporadaId(String temporadaId);

    @Query("""
        SELECT new com.ddo.torneios.dto.LeilaoResumoDTO(
            l.id, l.descricao, l.dataInicio, l.dataFim, l.ativo, l.selecao, l.temporada.id
        )
        FROM Leilao l
        WHERE l.temporada.id = :temporadaId
        ORDER BY l.dataInicio DESC
    """)
    List<LeilaoResumoDTO> listarResumoPorTemporada(@Param("temporadaId") String temporadaId);

}
