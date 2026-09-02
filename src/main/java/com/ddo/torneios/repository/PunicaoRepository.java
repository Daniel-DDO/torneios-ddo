package com.ddo.torneios.repository;

import com.ddo.torneios.model.Punicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PunicaoRepository extends JpaRepository<Punicao, String> {
    List<Punicao> findByParticipacaoFaseIdOrderByDataAplicacaoDesc(String participacaoFaseId);
    List<Punicao> findByParticipacaoFase_FaseId(String faseId);

    public interface PunicaoJogadorClubeProjection {
        String getJogadorClubeId();
        Integer getPontos();
    }

    @Query("""
    SELECT p.participacaoFase.jogadorClube.id AS jogadorClubeId, p.pontos AS pontos
    FROM Punicao p
    WHERE p.participacaoFase.fase.id = :faseId
""")
    List<PunicaoJogadorClubeProjection> buscarPunicoesPorFase(@Param("faseId") String faseId);
}