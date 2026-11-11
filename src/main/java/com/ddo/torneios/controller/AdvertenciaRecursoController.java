package com.ddo.torneios.controller;

import com.ddo.torneios.dto.AdvertenciaRecursoDTO;
import com.ddo.torneios.service.AdvertenciaRecursoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdvertenciaRecursoController {

    private final AdvertenciaRecursoService recursoService;

    public AdvertenciaRecursoController(AdvertenciaRecursoService recursoService) {
        this.recursoService = recursoService;
    }

    @GetMapping("/warnings/appeals/pending")
    public ResponseEntity<List<AdvertenciaRecursoDTO>> listarPendentes() {
        return ResponseEntity.ok(recursoService.listarPendentes());
    }
}
