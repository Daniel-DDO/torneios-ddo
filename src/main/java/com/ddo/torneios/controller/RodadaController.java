package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.dto.RodadaDTO;
import com.ddo.torneios.model.StatusRodada;
import com.ddo.torneios.service.RodadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rodada")
@RequiredArgsConstructor
public class RodadaController {

    private final RodadaService rodadaService;

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<Page<RodadaDTO>> listarRodadas(
            @PathVariable String faseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(rodadaService.listarRodadasPaginadas(faseId, pageable));
    }

    @GetMapping("/fase/{faseId}/numero/{numero}")
    public ResponseEntity<RodadaDTO> buscarPorNumero(
            @PathVariable String faseId,
            @PathVariable Integer numero
    ) {
        return ResponseEntity.ok(rodadaService.buscarPorNumero(faseId, numero));
    }

    @GetMapping("/fase/{faseId}/jogador/{jogadorId}")
    public ResponseEntity<List<PartidaDTO>> buscarPorJogador(
            @PathVariable String faseId,
            @PathVariable String jogadorId
    ) {
        return ResponseEntity.ok(rodadaService.buscarPartidasPorJogador(faseId, jogadorId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> atualizarStatus(
            @PathVariable String id,
            @RequestParam StatusRodada status
    ) {
        rodadaService.atualizarStatusRodada(id, status);
        return ResponseEntity.noContent().build();
    }
}