package com.ddo.torneios.controller;

import com.ddo.torneios.dto.AdvertenciaPublicaDTO;
import com.ddo.torneios.dto.AplicarAdvertenciaRequest;
import com.ddo.torneios.dto.CancelarAdvertenciaRequest;
import com.ddo.torneios.dto.DecidirRecursoRequest;
import com.ddo.torneios.dto.FichaAdvertenciasDTO;
import com.ddo.torneios.dto.InterporRecursoRequest;
import com.ddo.torneios.service.AdvertenciaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdvertenciaController {

    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final AdvertenciaService advertenciaService;

    public AdvertenciaController(AdvertenciaService advertenciaService) {
        this.advertenciaService = advertenciaService;
    }

    @GetMapping("/players/{jogadorId}/warnings")
    public ResponseEntity<FichaAdvertenciasDTO> buscarFicha(@PathVariable String jogadorId) {
        return ResponseEntity.ok(advertenciaService.buscarFicha(jogadorId));
    }

    @GetMapping("/players/{jogadorId}/warnings/history")
    public ResponseEntity<Page<AdvertenciaPublicaDTO>> listarHistorico(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        int tamanhoSeguro = Math.max(1, Math.min(tamanho, TAMANHO_MAXIMO_PAGINA));
        int paginaSegura = Math.max(0, pagina);
        return ResponseEntity.ok(advertenciaService.listarHistorico(jogadorId, PageRequest.of(paginaSegura, tamanhoSeguro)));
    }

    @PostMapping("/players/{jogadorId}/warnings")
    public ResponseEntity<AdvertenciaPublicaDTO> aplicar(
            @PathVariable String jogadorId,
            @Valid @RequestBody AplicarAdvertenciaRequest requisicao,
            Authentication autenticacao) {
        AdvertenciaPublicaDTO resposta = advertenciaService.aplicar(jogadorId, autenticacao.getName(), requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/warnings/{advertenciaId}/cancel")
    public ResponseEntity<AdvertenciaPublicaDTO> cancelar(
            @PathVariable String advertenciaId,
            @Valid @RequestBody CancelarAdvertenciaRequest requisicao,
            Authentication autenticacao) {
        return ResponseEntity.ok(advertenciaService.cancelar(advertenciaId, autenticacao.getName(), requisicao.motivo()));
    }

    @PostMapping("/warnings/{advertenciaId}/appeal")
    public ResponseEntity<AdvertenciaPublicaDTO> interporRecurso(
            @PathVariable String advertenciaId,
            @Valid @RequestBody InterporRecursoRequest requisicao,
            Authentication autenticacao) {
        return ResponseEntity.ok(advertenciaService.interporRecurso(advertenciaId, autenticacao.getName(), requisicao.texto()));
    }

    @PostMapping("/warnings/{advertenciaId}/appeal/decision")
    public ResponseEntity<AdvertenciaPublicaDTO> decidirRecurso(
            @PathVariable String advertenciaId,
            @Valid @RequestBody DecidirRecursoRequest requisicao,
            Authentication autenticacao) {
        return ResponseEntity.ok(advertenciaService.decidirRecurso(advertenciaId, autenticacao.getName(), requisicao));
    }
}
