package com.ddo.torneios.repository;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.Equipe;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.LigaClube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EquipeRepository extends JpaRepository<Equipe, String> {

    boolean existsByTorneioIdAndNome(String torneioId, String nome);
    boolean existsByTorneioIdAndNomeAndIdNot(String torneioId, String nome, String id);
    boolean existsByTorneioIdAndCapitaoId(String torneioId, String capitaoId);

    @Query("""
SELECT new com.ddo.torneios.dto.EquipeResumoDTO(e.id, e.nome, e.imagem, e.capitao.id, e.capitao.nome)
FROM Equipe e WHERE e.torneio.id = :torneioId ORDER BY e.nome
""")
    List<EquipeResumoDTO> listarPorTorneio(@Param("torneioId") String torneioId);

    @Query("""
SELECT new com.ddo.torneios.dto.EquipeResumoDTO(e.id, e.nome, e.imagem, e.capitao.id, e.capitao.nome)
FROM Equipe e WHERE e.id = :id
""")
    Optional<EquipeResumoDTO> buscarResumo(@Param("id") String id);

    @Query("SELECT new com.ddo.torneios.dto.EquipeRefDTO(e.torneio.id, e.capitao.id) FROM Equipe e WHERE e.id = :id")
    Optional<EquipeRefDTO> buscarRef(@Param("id") String id);

    @Query("SELECT e.imagem FROM Equipe e WHERE e.id = :id")
    Optional<String> buscarImagem(@Param("id") String id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Equipe e SET e.nome = :nome WHERE e.id = :id")
    int atualizarNome(@Param("id") String id, @Param("nome") String nome);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Equipe e SET e.imagem = :imagem WHERE e.id = :id")
    int atualizarImagem(@Param("id") String id, @Param("imagem") String imagem);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Equipe e SET e.capitao = :capitao WHERE e.id = :id")
    int atualizarCapitao(@Param("id") String id, @Param("capitao") Jogador capitao);

}