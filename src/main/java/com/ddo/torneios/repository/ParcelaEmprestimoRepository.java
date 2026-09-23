package com.ddo.torneios.repository;

import com.ddo.torneios.model.ParcelaEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParcelaEmprestimoRepository extends JpaRepository<ParcelaEmprestimo, String> {

    /** Parcelas pendentes cujo vencimento já chegou (job automático) */
    @Query("""
        SELECT p FROM ParcelaEmprestimo p
        JOIN FETCH p.emprestimo e
        JOIN FETCH e.jogador
        WHERE p.paga = false
          AND p.dataVencimento <= :agora
          AND e.status = com.ddo.torneios.model.StatusEmprestimo.EM_ANDAMENTO
        ORDER BY p.dataVencimento ASC
    """)
    List<ParcelaEmprestimo> buscarParcelasVencidasPendentes(@Param("agora") LocalDateTime agora);

    /** Parcelas que vencem exatamente hoje, para a cobrança forçada do PROPRIETARIO */
    @Query("""
        SELECT p FROM ParcelaEmprestimo p
        JOIN FETCH p.emprestimo e
        JOIN FETCH e.jogador
        WHERE p.paga = false
          AND p.dataVencimento >= :inicioDia
          AND p.dataVencimento < :fimDia
          AND e.status = com.ddo.torneios.model.StatusEmprestimo.EM_ANDAMENTO
        ORDER BY p.dataVencimento ASC
    """)
    List<ParcelaEmprestimo> buscarParcelasQueVencemHoje(@Param("inicioDia") LocalDateTime inicioDia,
                                                        @Param("fimDia") LocalDateTime fimDia);

    Optional<ParcelaEmprestimo> findByIdAndPagaFalse(String id);

    @Query("""
        SELECT p FROM ParcelaEmprestimo p
        WHERE p.emprestimo.id = :emprestimoId AND p.paga = false
        ORDER BY p.numeroParcela ASC
    """)
    List<ParcelaEmprestimo> buscarProximasPendentes(@Param("emprestimoId") String emprestimoId);
}