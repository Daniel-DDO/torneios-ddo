package com.ddo.torneios.repository;

import com.ddo.torneios.model.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticiaRepository extends JpaRepository<Noticia, String> {
    List<Noticia> findTop10ByOrderByDataCriacaoDesc();
}