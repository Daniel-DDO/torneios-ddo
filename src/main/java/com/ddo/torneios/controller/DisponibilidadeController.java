package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.service.DisponibilidadeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/disponibilidades")
public class DisponibilidadeController {

    private static final Set<String> CARGOS_STAFF = Set.of("PROPRIETARIO", "DIRETOR", "ADMINISTRADOR");

    @Autowired private DisponibilidadeService disponibilidadeService;

    // Authentication.getName() = Jogador.getUsername() = id do jogador

    @GetMapping("/me")
    public ResponseEntity<DisponibilidadeDTO> minhaDisponibilidade(Authentication authentication) {
        return ResponseEntity.ok(disponibilidadeService.buscarAtual(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<DisponibilidadeDTO> atualizarMinhaGrade(
            Authentication authentication,
            @Valid @RequestBody DisponibilidadeRequestDTO request) {
        return ResponseEntity.ok(disponibilidadeService.atualizarGrade(authentication.getName(), request));
    }

    /** A grade atual é visível pra qualquer usuário autenticado (o adversário precisa enxergar). */
    @GetMapping("/jogador/{jogadorId}")
    public ResponseEntity<DisponibilidadeDTO> disponibilidadeDoJogador(@PathVariable String jogadorId) {
        return ResponseEntity.ok(disponibilidadeService.buscarAtual(jogadorId));
    }

    /** Log de alterações: só o próprio jogador e a organização. */
    @GetMapping("/jogador/{jogadorId}/historico")
    public ResponseEntity<Page<DisponibilidadeHistoricoDTO>> historico(
            Authentication authentication,
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        exigirDonoOuStaff(authentication, jogadorId);
        return ResponseEntity.ok(disponibilidadeService.listarHistorico(jogadorId, page, size));
    }

    @PostMapping("/me/observacoes")
    public ResponseEntity<ObservacaoDisponibilidadeDTO> registrarObservacao(
            Authentication authentication,
            @Valid @RequestBody ObservacaoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadeService.registrarObservacao(authentication.getName(), request.texto()));
    }

    /** Observações: só o próprio jogador e a organização. */
    @GetMapping("/jogador/{jogadorId}/observacoes")
    public ResponseEntity<Page<ObservacaoDisponibilidadeDTO>> observacoes(
            Authentication authentication,
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        exigirDonoOuStaff(authentication, jogadorId);
        return ResponseEntity.ok(disponibilidadeService.listarObservacoes(jogadorId, page, size));
    }

    private void exigirDonoOuStaff(Authentication authentication, String jogadorId) {
        boolean dono = authentication.getName().equals(jogadorId);
        boolean staff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(CARGOS_STAFF::contains);

        if (!dono && !staff) {
            throw new AccessDeniedException("Sem permissão para ver esse registro.");
        }
    }
}
