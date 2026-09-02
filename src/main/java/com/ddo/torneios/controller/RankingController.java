package com.ddo.torneios.controller;

import com.ddo.torneios.dto.RankingDetalheDTO;
import com.ddo.torneios.service.RankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    // ---------- consulta ----------

    @GetMapping("/jogador/{jogadorId}")
    public ResponseEntity<RankingDetalheDTO> detalhe(@PathVariable String jogadorId) {
        return ResponseEntity.ok(rankingService.obterDetalhe(jogadorId));
    }

    @GetMapping("/tabela")
    public ResponseEntity<List<RankingDetalheDTO>> tabela() {
        return ResponseEntity.ok(rankingService.obterTabela());
    }

    // ---------- administração ----------

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/admin/zerar/{jogadorId}")
    public ResponseEntity<Void> zerarUm(@PathVariable String jogadorId) {
        rankingService.zerarRanking(jogadorId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/admin/zerar-todos")
    public ResponseEntity<Void> zerarTodos() {
        rankingService.zerarRankingTodos();
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/admin/recalcular/{jogadorId}")
    public ResponseEntity<RankingDetalheDTO> recalcularUm(@PathVariable String jogadorId) {
        rankingService.recalcularApartirHistorico(jogadorId);
        return ResponseEntity.ok(rankingService.obterDetalhe(jogadorId));
    }

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/admin/recalcular-todos")
    public ResponseEntity<Void> recalcularTodos() {
        rankingService.recalcularTodos();
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    @PostMapping("/admin/decaimento-temporada")
    public ResponseEntity<Void> decaimentoTemporada(@RequestParam(defaultValue = "1") int ranksParaCair) {
        rankingService.aplicarDecaimentoTemporada(ranksParaCair);
        return ResponseEntity.noContent().build();
    }
}