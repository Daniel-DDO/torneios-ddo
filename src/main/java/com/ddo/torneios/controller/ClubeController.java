package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.LigaClube;
import com.ddo.torneios.request.ClubeRequest;
import com.ddo.torneios.service.ClubeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PatchMapping("/{id}")
    public ResponseEntity<Clube> atualizarClube(@PathVariable String id, @RequestBody ClubeRequest request) {
        Clube clubeAtualizado = clubeService.atualizarClube(id, request);
        return ResponseEntity.ok(clubeAtualizado);
    }

    @GetMapping("/buscar-autocomplete")
    public ResponseEntity<List<Clube>> buscarAutocomplete(@RequestParam String termo) {
        List<Clube> sugestoes = clubeService.buscarAutocomplete(termo);
        return ResponseEntity.ok(sugestoes);
    }

    @GetMapping("/selecoes")
    public PaginacaoDTO<Clube> getSelecoes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarSomenteSelecoes(page, size);
    }

    @GetMapping("/clubes")
    public PaginacaoDTO<Clube> getClubesExcetoSelecao(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarExcetoSelecoes(page, size);
    }

    @GetMapping("/liga/{liga}")
    public PaginacaoDTO<Clube> getPorLiga(
            @PathVariable LigaClube liga,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarPorLiga(liga, page, size);
    }

    @GetMapping("/estatisticas/contagem/{liga}")
    public ResponseEntity<Long> getContagemPorLiga(@PathVariable LigaClube liga) {
        return ResponseEntity.ok(clubeService.contarClubesPorLiga(liga));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable String id) {
        clubeService.alternarStatusAtivo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rankings/titulos")
    public List<Clube> getTopVencedores(@RequestParam(defaultValue = "5") int limit) {
        return clubeService.listarTopVencedores(limit);
    }
}
