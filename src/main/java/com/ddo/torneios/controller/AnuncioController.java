package com.ddo.torneios.controller;

import com.ddo.torneios.dto.AnuncioDTO;
import com.ddo.torneios.request.AnuncioRequest;
import com.ddo.torneios.service.AnuncioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/anuncios")
@RequiredArgsConstructor
public class AnuncioController {

    private final AnuncioService anuncioService;

    @PostMapping
    public ResponseEntity<AnuncioDTO> criarAnuncio(
            @RequestBody @Valid AnuncioRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        AnuncioDTO response = anuncioService.criarAnuncio(request);

        URI uri = uriBuilder.path("/anuncios/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnuncioDTO> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(anuncioService.buscarPorId(id));
    }

    @GetMapping("/recentes")
    public ResponseEntity<List<AnuncioDTO>> listarUltimos10() {
        return ResponseEntity.ok(anuncioService.listarUltimos10());
    }

    @GetMapping("/ultimo")
    public ResponseEntity<AnuncioDTO> buscarMaisRecente() {
        return ResponseEntity.ok(anuncioService.buscarMaisRecente());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<AnuncioDTO>> buscarPorTitulo(@RequestParam String termo) {
        return ResponseEntity.ok(anuncioService.buscarPorTitulo(termo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnuncioDTO> atualizarAnuncio(@PathVariable String id, @RequestBody @Valid AnuncioRequest request) {
        return ResponseEntity.ok(anuncioService.atualizarAnuncio(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirAnuncio(@PathVariable String id) {
        anuncioService.excluirAnuncio(id);
        return ResponseEntity.noContent().build();
    }
}