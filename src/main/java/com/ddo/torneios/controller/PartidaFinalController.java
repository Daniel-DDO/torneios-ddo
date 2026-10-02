package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PartidaFinalDTO;
import com.ddo.torneios.service.PartidaFinalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partida/finals")
@RequiredArgsConstructor
public class PartidaFinalController {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final PartidaFinalService partidaFinalService;

    @GetMapping
    public ResponseEntity<Page<PartidaFinalDTO>> listarFinais(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidaFinalService.listarFinais(paginar(page, size)));
    }

    @GetMapping("/played")
    public ResponseEntity<Page<PartidaFinalDTO>> listarFinaisRealizadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidaFinalService.listarFinaisRealizadas(paginar(page, size)));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<PartidaFinalDTO>> listarFinaisPendentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidaFinalService.listarFinaisPendentes(paginar(page, size)));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<Page<PartidaFinalDTO>> listarFinaisDoJogador(
            @PathVariable String playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(partidaFinalService.listarFinaisDoJogador(playerId, paginar(page, size)));
    }

    private Pageable paginar(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANHO_MAXIMO_PAGINA));
    }
}