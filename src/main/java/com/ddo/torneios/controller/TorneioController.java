package com.ddo.torneios.controller;

import com.ddo.torneios.dto.TorneioDTO;
import com.ddo.torneios.request.TorneioRequest;
import com.ddo.torneios.service.TorneioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/torneio")
public class TorneioController {

    @Autowired
    private TorneioService torneioService;

    @PostMapping("/criar")
    public ResponseEntity<TorneioDTO> criarTorneio(@RequestBody @Valid TorneioRequest request) {
        TorneioDTO novoTorneio = torneioService.criarTorneio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoTorneio);
    }

    @GetMapping("/all")
    public ResponseEntity<List<TorneioDTO>> listarTodos() {
        List<TorneioDTO> torneios = torneioService.listarTodos();
        return ResponseEntity.ok(torneios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TorneioDTO> buscarPorId(@PathVariable String id) {
        TorneioDTO torneio = torneioService.buscarTorneioPorId(id);
        return ResponseEntity.ok(torneio);
    }

    @GetMapping("/temporada/{temporadaId}")
    public ResponseEntity<List<TorneioDTO>> listarPorTemporada(@PathVariable String temporadaId) {
        List<TorneioDTO> torneios = torneioService.listarPorTemporada(temporadaId);
        return ResponseEntity.ok(torneios);
    }

}