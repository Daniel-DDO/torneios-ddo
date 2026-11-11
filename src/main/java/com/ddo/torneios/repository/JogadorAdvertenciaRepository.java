package com.ddo.torneios.repository;

import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface JogadorAdvertenciaRepository extends JpaRepository<Jogador, String> {

    @Query(value = "select j.id from jogador j where j.id = :id for update", nativeQuery = true)
    Optional<String> travarPorId(@Param("id") String id);

    @Query("select j.cargo from Jogador j where j.id = :id")
    Optional<Cargo> buscarCargo(@Param("id") String id);

    @Query("select j.nome from Jogador j where j.id = :id")
    Optional<String> buscarNome(@Param("id") String id);

    @Query("select j.saldoVirtual from Jogador j where j.id = :id")
    Optional<BigDecimal> buscarSaldo(@Param("id") String id);

    @Query("select j.negativado from Jogador j where j.id = :id")
    boolean estaNegativado(@Param("id") String id);

    @Modifying
    @Query("update Jogador j set j.saldoVirtual = coalesce(j.saldoVirtual, 0) + :delta where j.id = :id")
    int ajustarSaldo(@Param("id") String id, @Param("delta") BigDecimal delta);

    @Modifying
    @Query("update Jogador j set j.negativado = true, j.dataNegativacao = :agora, "
            + "j.jaFoiNegativadoAlgumaVez = true where j.id = :id")
    int marcarNegativado(@Param("id") String id, @Param("agora") LocalDateTime agora);

    @Modifying
    @Query("update Jogador j set j.negativado = false, j.dataQuitacaoNegativacao = :agora where j.id = :id")
    int reverterNegativado(@Param("id") String id, @Param("agora") LocalDateTime agora);
}
