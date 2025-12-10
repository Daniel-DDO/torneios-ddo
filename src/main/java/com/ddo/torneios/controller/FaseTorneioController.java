package com.ddo.torneios.controller;

import com.ddo.torneios.dto.FaseTorneioDTO;
import com.ddo.torneios.request.FaseTorneioRequest;
import com.ddo.torneios.service.FaseTorneioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fase-torneio")
public class FaseTorneioController {

    @Autowired
    private FaseTorneioService faseTorneioService;

    @PostMapping("/criar")
    public ResponseEntity<FaseTorneioDTO> criarFase(@RequestBody @Valid FaseTorneioRequest request) {
        FaseTorneioDTO novaFase = faseTorneioService.criarFase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaFase);
    }

    @GetMapping("/torneio/{torneioId}")
    public ResponseEntity<List<FaseTorneioDTO>> listarPorTorneio(@PathVariable String torneioId) {
        List<FaseTorneioDTO> fases = faseTorneioService.listarFasesDoTorneio(torneioId);
        return ResponseEntity.ok(fases);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarFase(@PathVariable String id) {
        faseTorneioService.deletarFase(id);
        return ResponseEntity.noContent().build();
    }
}