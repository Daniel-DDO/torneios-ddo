package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.request.ClubeRequest;
import com.ddo.torneios.service.ClubeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clube")
public class ClubeController {

    @Autowired
    private ClubeService clubeService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarClube(@Valid @RequestBody ClubeRequest request) {
        clubeService.cadastrarClube(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/all")
    public ResponseEntity<PaginacaoDTO<Clube>> listarClubes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String nomeFiltro
    ) {
        PaginacaoDTO<Clube> pagina = clubeService.listarClubes(nomeFiltro, page, size, sortBy, direction);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Clube> retornarClube(@PathVariable String id) {
        return clubeService.retornarClube(id);
    }
}
