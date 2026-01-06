package com.ddo.torneios.controller;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.request.GerarCopaRealRequest;
import com.ddo.torneios.service.gerador.GeradorCopaRealStrategy;
import com.ddo.torneios.service.gerador.GeradorPartidasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fases")
@RequiredArgsConstructor
public class GeradorController {

    private final GeradorPartidasService geradorService;
    private final FaseTorneioRepository faseRepository;
    private final ParticipacaoFaseRepository participacaoRepository;
    private final PartidaRepository partidaRepository;
    private final GeradorCopaRealStrategy geradorCopaRealStrategy;

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

    @PostMapping("/gerar-copa-real")
    public ResponseEntity<List<Partida>> gerarCopaReal(@RequestBody GerarCopaRealRequest request) {

        FaseTorneio fase = faseRepository.findById(request.getFaseId())
                .orElseThrow(() -> new RuntimeException("Fase não encontrada"));

        geradorService.limparGeracoesAnteriores(fase);

        List<ParticipacaoFase> elite = participacaoRepository.findAllById(request.getIdsElite());
        List<ParticipacaoFase> intermed = participacaoRepository.findAllById(request.getIdsIntermediarios());
        List<ParticipacaoFase> resto = participacaoRepository.findAllById(request.getIdsResto());

        if(elite.size() != 8) throw new RuntimeException("Erro: Lista Elite precisa de 8 IDs válidos.");
        if(intermed.size() != 8) throw new RuntimeException("Erro: Lista Intermediária precisa de 8 IDs válidos.");

        List<Partida> partidasGeradas = geradorCopaRealStrategy.gerar(fase, elite, intermed, resto);

        partidaRepository.saveAll(partidasGeradas);

        return ResponseEntity.ok(partidasGeradas);
    }
}
