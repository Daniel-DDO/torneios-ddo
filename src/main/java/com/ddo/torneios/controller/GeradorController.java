package com.ddo.torneios.controller;

import com.ddo.torneios.model.AlgoritmoGeracaoLiga;
import com.ddo.torneios.model.AlgoritmoGeracaoMataMata;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.service.gerador.GeradorPartidasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fases")
@RequiredArgsConstructor
public class GeradorController {

    private final GeradorPartidasService geradorService;

    @GetMapping("/algoritmos")
    public ResponseEntity<?> listarAlgoritmos() {
        return ResponseEntity.ok(Map.of(
                "liga", AlgoritmoGeracaoLiga.values(),
                "mataMata", AlgoritmoGeracaoMataMata.values()
        ));
    }

    @PostMapping("/{faseId}/gerar")
    public ResponseEntity<?> gerarSorteio(
            @PathVariable String faseId,
            @RequestParam(required = false) AlgoritmoGeracaoMataMata algoritmoMataMata,
            @RequestParam(required = false) AlgoritmoGeracaoLiga algoritmoLiga) {
        try {
            geradorService.gerarEstruturaFase(faseId, algoritmoMataMata, algoritmoLiga);
            return ResponseEntity.ok(Map.of("message", "Sorteio realizado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{faseId}/limpar")
    public ResponseEntity<?> limparFase(@PathVariable String faseId) {
        try {
            FaseTorneio fase = geradorService.buscarPorId(faseId)
                    .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada com o ID: " + faseId));

            geradorService.limparGeracoesAnteriores(fase);

            return ResponseEntity.ok(Map.of("message", "Fase resetada com sucesso!"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro inesperado: " + e.getMessage()));
        }
    }
}