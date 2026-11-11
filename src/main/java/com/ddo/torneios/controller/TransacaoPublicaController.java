package com.ddo.torneios.controller;

import com.ddo.torneios.dto.TransacaoPublicaDTO;
import com.ddo.torneios.service.TransacaoPublicaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransacaoPublicaController {

    private final TransacaoPublicaService transacaoService;

    public TransacaoPublicaController(TransacaoPublicaService transacaoService) {
        this.transacaoService = transacaoService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransacaoPublicaDTO>> listarUltimas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transacaoService.listarUltimas(page, size));
    }
}
