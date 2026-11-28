package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.service.AmistosoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/amistosos")
public class AmistosoController {

    @Autowired
    private AmistosoService amistosoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PROPRIETARIO','DIRETOR')")
    public ResponseEntity<AmistosoDTO> criar(@Valid @RequestBody CriarAmistosoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amistosoService.criar(req));
    }

    // Registra o resultado; chamar de novo corrige o resultado já registrado
    @PutMapping("/{id}/resultado")
    @PreAuthorize("hasAnyRole('PROPRIETARIO','DIRETOR','ADMINISTRADOR')")
    public ResponseEntity<AmistosoDTO> registrarResultado(@PathVariable String id,
                                                          @Valid @RequestBody RegistrarResultadoAmistosoRequest req) {
        return ResponseEntity.ok(amistosoService.registrarResultado(id, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmistosoDTO> buscar(@PathVariable String id) {
        return ResponseEntity.ok(amistosoService.buscarPorId(id));
    }

    // ?realizada=true|false filtra; sem parâmetro lista todos
    @GetMapping
    public ResponseEntity<PaginacaoDTO<AmistosoDTO>> listar(
            @RequestParam(required = false) Boolean realizada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(amistosoService.listar(realizada, pagina, tamanho));
    }

    // ?realizada=true (jogados) | false (agendados); sem parâmetro lista todos do jogador
    @GetMapping("/jogador/{jogadorId}")
    public ResponseEntity<PaginacaoDTO<AmistosoDTO>> listarPorJogador(
            @PathVariable String jogadorId,
            @RequestParam(required = false) Boolean realizada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(amistosoService.listarPorJogador(jogadorId, realizada, pagina, tamanho));
    }

    @GetMapping("/jogador/{jogadorId}/estatisticas")
    public ResponseEntity<AmistosoEstatisticaDTO> estatisticas(@PathVariable String jogadorId) {
        return ResponseEntity.ok(amistosoService.estatisticasDoJogador(jogadorId));
    }
}