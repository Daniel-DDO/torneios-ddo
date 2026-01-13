package com.ddo.torneios.repository;

import com.ddo.torneios.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConquistaRepository extends JpaRepository<Conquista, String> {
    List<Conquista> findTop10ByOrderByDataConquistaDesc();
}