package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PunicaoDTO;
import com.ddo.torneios.request.PunicaoRequest;
import com.ddo.torneios.service.PunicaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/punicoes")
public class PunicaoController {

    @Autowired
    private PunicaoService punicaoService;

    @PostMapping
    public ResponseEntity<PunicaoDTO> aplicar(@RequestBody @Valid PunicaoRequest request) {
        return ResponseEntity.ok(punicaoService.aplicarPunicao(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        punicaoService.removerPunicao(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/participacao/{participacaoId}")
    public ResponseEntity<List<PunicaoDTO>> listarPorParticipacao(@PathVariable String participacaoId) {
        return ResponseEntity.ok(punicaoService.listarPorParticipacao(participacaoId));
    }
}