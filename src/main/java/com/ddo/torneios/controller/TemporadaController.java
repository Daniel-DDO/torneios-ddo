package com.ddo.torneios.controller;

import com.ddo.torneios.dto.TemporadaDTO;
import com.ddo.torneios.request.TemporadaRequest;
import com.ddo.torneios.service.TemporadaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/temporada")
public class TemporadaController {

    @Autowired
    private TemporadaService temporadaService;

    @PostMapping("/criar")
    public ResponseEntity<TemporadaDTO> criarTemporada(@RequestBody @Valid TemporadaRequest request) {
        TemporadaDTO novaTemporada = temporadaService.criarTemporada(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaTemporada);
    }

    @GetMapping("/all")
    public ResponseEntity<List<TemporadaDTO>> listarTodas() {
        List<TemporadaDTO> temporadas = temporadaService.listarTodas();
        return ResponseEntity.ok(temporadas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemporadaDTO> buscarPorId(@PathVariable String id) {
        TemporadaDTO temporada = temporadaService.buscarPorId(id);
        return ResponseEntity.ok(temporada);
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<TemporadaDTO> buscarPorNome(@PathVariable String nome) {
        TemporadaDTO temporada = temporadaService.buscarPorNome(nome);
        return ResponseEntity.ok(temporada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemporadaDTO> atualizarTemporada(@PathVariable String id,
                                                           @RequestBody @Valid TemporadaRequest request) {
        TemporadaDTO temporadaAtualizada = temporadaService.atualizarTemporada(id, request);
        return ResponseEntity.ok(temporadaAtualizada);
    }

    @PatchMapping("/{id}/encerrar")
    public ResponseEntity<TemporadaDTO> encerrarTemporada(@PathVariable String id) {
        TemporadaDTO temporadaEncerrada = temporadaService.encerrarTemporada(id);
        return ResponseEntity.ok(temporadaEncerrada);
    }

}