package com.ddo.torneios.controller;

import com.ddo.torneios.dto.JogadorClubeDTO;
import com.ddo.torneios.request.JogadorClubeRequest;
import com.ddo.torneios.service.JogadorClubeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inscricao")
public class JogadorClubeController {

    @Autowired
    private JogadorClubeService jogadorClubeService;

    @PostMapping("/inscrever")
    public ResponseEntity<JogadorClubeDTO> realizarInscricao(@RequestBody @Valid JogadorClubeRequest request) {
        JogadorClubeDTO dto = jogadorClubeService.inscreverJogador(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/temporada/{temporadaId}")
    public ResponseEntity<List<JogadorClubeDTO>> listarInscritos(@PathVariable String temporadaId) {
        List<JogadorClubeDTO> inscritos = jogadorClubeService.listarInscritosPorTemporada(temporadaId);
        return ResponseEntity.ok(inscritos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarInscricao(@PathVariable String id) {
        jogadorClubeService.removerInscricao(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/torneio/{torneioId}")
    public ResponseEntity<List<JogadorClubeDTO>> listarInscritosPorTorneio(@PathVariable String torneioId) {
        List<JogadorClubeDTO> inscritos = jogadorClubeService.listarInscritosPorTorneio(torneioId);
        return ResponseEntity.ok(inscritos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<JogadorClubeDTO>> listarTodos() {
        List<JogadorClubeDTO> lista = jogadorClubeService.listarTodos();
        return ResponseEntity.ok(lista);
    }
}