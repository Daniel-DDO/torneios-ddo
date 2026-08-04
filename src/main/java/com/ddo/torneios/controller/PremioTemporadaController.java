package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PremioTemporadaDTO;
import com.ddo.torneios.service.PremioTemporadaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/premios-temporada")
public class PremioTemporadaController {

    @Autowired
    private PremioTemporadaService premioTemporadaService;

    @GetMapping("/{temporadaId}")
    public ResponseEntity<List<PremioTemporadaDTO>> listar(@PathVariable String temporadaId) {
        return ResponseEntity.ok(premioTemporadaService.obterPremiosDaTemporada(temporadaId));
    }

    @GetMapping("/jogador/{jogadorId}")
    public ResponseEntity<List<PremioTemporadaDTO>> listarDoJogador(@PathVariable String jogadorId) {
        return ResponseEntity.ok(premioTemporadaService.obterPremiosDoJogador(jogadorId));
    }

    @GetMapping("/preview/{temporadaId}")
    public ResponseEntity<List<PremioTemporadaDTO>> preview(@PathVariable String temporadaId) {
        return ResponseEntity.ok(premioTemporadaService.previewCampeoesDaTemporada(temporadaId));
    }

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/apurar/{temporadaId}")
    public ResponseEntity<List<PremioTemporadaDTO>> apurar(@PathVariable String temporadaId) {
        return ResponseEntity.ok(premioTemporadaService.apurarPremiosDaTemporada(temporadaId));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @DeleteMapping("/apurar/{temporadaId}")
    public ResponseEntity<Void> removerApuracao(@PathVariable String temporadaId) {
        premioTemporadaService.removerApuracao(temporadaId);
        return ResponseEntity.noContent().build();
    }
}