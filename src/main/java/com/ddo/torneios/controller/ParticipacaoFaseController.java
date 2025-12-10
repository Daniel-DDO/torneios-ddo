package com.ddo.torneios.controller;

import com.ddo.torneios.dto.ParticipacaoFaseDTO;
import com.ddo.torneios.request.ParticipacaoFaseRequest;
import com.ddo.torneios.service.ParticipacaoFaseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/participacao-fase")
public class ParticipacaoFaseController {

    @Autowired
    private ParticipacaoFaseService participacaoFaseService;

    @PostMapping("/add")
    public ResponseEntity<ParticipacaoFaseDTO> adicionarParticipante(@RequestBody @Valid ParticipacaoFaseRequest request) {
        ParticipacaoFaseDTO dto = participacaoFaseService.adicionarParticipante(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<List<ParticipacaoFaseDTO>> listarClassificacao(@PathVariable String faseId) {
        List<ParticipacaoFaseDTO> classificacao = participacaoFaseService.listarClassificacao(faseId);
        return ResponseEntity.ok(classificacao);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerParticipante(@PathVariable String id) {
        participacaoFaseService.removerParticipante(id);
        return ResponseEntity.noContent().build();
    }
}