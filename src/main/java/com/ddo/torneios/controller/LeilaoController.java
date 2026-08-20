package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.request.IniciarLeilaoRequest;
import com.ddo.torneios.service.LeilaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/leiloes")
public class LeilaoController {

    @Autowired
    private LeilaoService leilaoService;

    @PostMapping("/lance")
    public ResponseEntity<Void> realizarLance(@RequestBody @Valid RealizarLanceDTO dto, Authentication auth) {
        leilaoService.registrarLances(auth.getName(), dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lance/v2")
    public ResponseEntity<Void> realizarLanceV2(@RequestBody @Valid RealizarLanceDTO dto, Authentication auth) {
        leilaoService.registrarLancesBlindado(auth.getName(), dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{leilaoId}/disputa/{clubeId}")
    public ResponseEntity<DisputaClubeDTO> verDisputaClube(@PathVariable String leilaoId, @PathVariable String clubeId) {
        return ResponseEntity.ok(leilaoService.obterDetalhesDisputa(leilaoId, clubeId));
    }

    @PostMapping("/admin/iniciar")
    public ResponseEntity<LeilaoResumoDTO> iniciarLeilao(@RequestBody @Valid IniciarLeilaoRequest request) {
        LocalDateTime dataFim = LocalDateTime.now().plusHours(request.horasDuracao());
        return ResponseEntity.ok(leilaoService.iniciarLeilao(request.temporadaId(), dataFim, request.isSelecao()));
    }

    @PostMapping("/admin/{id}/finalizar")
    public ResponseEntity<String> finalizarLeilao(@PathVariable String id) {
        leilaoService.finalizarLeilao(id);
        return ResponseEntity.ok("Leilão finalizado e processado com sucesso. Vencedores definidos.");
    }

    @GetMapping("/{id}/lances-atuais")
    public ResponseEntity<List<LanceResumoDTO>> verLancesAtuais(@PathVariable String id) {
        return ResponseEntity.ok(leilaoService.obterLancesAtuais(id));
    }

    @GetMapping("/{leilaoId}/historico/{clubeId}")
    public ResponseEntity<List<HistoricoLancesClubeDTO>> verHistoricoDoClube(
            @PathVariable String leilaoId,
            @PathVariable String clubeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leilaoService.obterHistoricoLances(leilaoId, clubeId, page, size));
    }

    @GetMapping("/{leilaoId}/resultado-oficial")
    public ResponseEntity<List<ResultadoLeilaoDTO>> verResultadoOficial(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.obterResultadoFinal(leilaoId));
    }

    @GetMapping("/{id}/meu-status")
    public ResponseEntity<List<StatusLanceJogadorDTO>> verMeuStatus(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(leilaoService.obterStatusDoJogador(id, auth.getName()));
    }

    @GetMapping("/temporada/{temporadaId}")
    public ResponseEntity<List<LeilaoResumoDTO>> listarPorTemporada(@PathVariable String temporadaId) {
        return ResponseEntity.ok(leilaoService.listarPorTemporada(temporadaId));
    }

    @GetMapping("/temporada/{temporadaId}/existe")
    public ResponseEntity<Boolean> verificarSeExisteLeilao(@PathVariable String temporadaId) {
        return ResponseEntity.ok(leilaoService.existeLeilaoParaTemporada(temporadaId));
    }

    @GetMapping("/{leilaoId}/meus-lances")
    public ResponseEntity<List<LanceDetalheDTO>> verMeusLancesParaEdicao(@PathVariable String leilaoId, Authentication auth) {
        return ResponseEntity.ok(leilaoService.buscarLancesDoJogador(leilaoId, auth.getName()));
    }

    @GetMapping("/{leilaoId}/feed")
    public ResponseEntity<List<FeedItemDTO>> getFeedInicial(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.obterFeedInicial(leilaoId));
    }

    @GetMapping("/{leilaoId}/mais-disputados")
    public ResponseEntity<List<ClubeDisputadoDTO>> getMaisDisputados(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.obterTermometro(leilaoId));
    }

    @DeleteMapping("/{leilaoId}/meus-lances")
    public ResponseEntity<Void> resetarMeusLances(@PathVariable String leilaoId, Authentication auth) {
        leilaoService.resetarLancesDoJogador(leilaoId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{leilaoId}/resultados-parciais")
    public ResponseEntity<List<ResultadoParcialDTO>> verResultadosParciais(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.calcularResultadosParciais(leilaoId));
    }
}