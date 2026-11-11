package com.ddo.torneios.repository;

import com.ddo.torneios.dto.AdvertenciaRecursoDTO;
import com.ddo.torneios.model.Advertencia;
import com.ddo.torneios.model.StatusAdvertencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdvertenciaRecursoRepository extends JpaRepository<Advertencia, String> {

    @Query("select new com.ddo.torneios.dto.AdvertenciaRecursoDTO(a.id, a.jogador.id, a.jogador.nome, a.nivel, "
            + "a.fato, a.artigo, a.multiplicador, a.multaFinal, a.oficializadaEm, a.recursoTexto, a.recursoEm) "
            + "from Advertencia a where a.recursoInterposto = true and a.recursoDecididoEm is null "
            + "and a.status <> :cancelada order by a.recursoEm asc")
    List<AdvertenciaRecursoDTO> buscarRecursosPendentes(@Param("cancelada") StatusAdvertencia cancelada);
}
