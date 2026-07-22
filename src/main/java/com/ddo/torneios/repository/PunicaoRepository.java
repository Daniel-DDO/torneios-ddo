package com.ddo.torneios.repository;

import com.ddo.torneios.model.Punicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PunicaoRepository extends JpaRepository<Punicao, String> {
    List<Punicao> findByParticipacaoFaseIdOrderByDataAplicacaoDesc(String participacaoFaseId);
    List<Punicao> findByParticipacaoFase_FaseId(String faseId);
}