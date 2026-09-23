package com.ddo.torneios.repository;

import com.ddo.torneios.model.Emprestimo;
import com.ddo.torneios.model.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, String> {

    Optional<Emprestimo> findByJogador_IdAndStatus(String jogadorId, StatusEmprestimo status);

    boolean existsByJogador_IdAndStatus(String jogadorId, StatusEmprestimo status);

    @Query("""
        SELECT e FROM Emprestimo e
        JOIN FETCH e.jogador
        LEFT JOIN FETCH e.parcelas
        WHERE e.id = :id
    """)
    Optional<Emprestimo> buscarComParcelas(@Param("id") String id);

    @Query("""
        SELECT e FROM Emprestimo e
        JOIN FETCH e.jogador
        LEFT JOIN FETCH e.parcelas
        WHERE e.jogador.id = :jogadorId
        ORDER BY e.dataContratacao DESC
    """)
    List<Emprestimo> buscarHistoricoComParcelas(@Param("jogadorId") String jogadorId);
}