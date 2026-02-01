package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Leilao;
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
    public ResponseEntity<Void> realizarLance(@RequestBody @Valid RealizarLanceDTO dto,
                                              Authentication authentication) {

        String jogadorId = authentication.getName();
        leilaoService.registrarLances(jogadorId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{leilaoId}/disputa/{clubeId}")
    public ResponseEntity<DisputaClubeDTO> verDisputaClube(@PathVariable String leilaoId,
                                                           @PathVariable String clubeId) {
        DisputaClubeDTO dto = leilaoService.obterDetalhesDisputa(leilaoId, clubeId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/admin/iniciar")
    public ResponseEntity<Leilao> iniciarLeilao(@RequestBody @Valid IniciarLeilaoRequest request) {

        LocalDateTime dataFim = LocalDateTime.now().plusHours(request.horasDuracao());
        Leilao leilao = leilaoService.iniciarLeilao(request.temporadaId(), dataFim, request.isSelecao());
        return ResponseEntity.ok(leilao);
    }

    @PostMapping("/admin/{id}/finalizar")
    public ResponseEntity<String> finalizarLeilao(@PathVariable String id) {
        leilaoService.finalizarLeilao(id);
        return ResponseEntity.ok("Leilão finalizado e processado com sucesso. Vencedores definidos.");
    }

    @GetMapping("/{id}/lances-atuais")
    public ResponseEntity<List<LanceResumoDTO>> verLancesAtuais(@PathVariable String id) {
        var lances = leilaoService.obterLancesAtuais(id);
        return ResponseEntity.ok(lances);
    }

    @GetMapping("/{leilaoId}/historico/{clubeId}")
    public ResponseEntity<List<HistoricoLancesClubeDTO>> verHistoricoDoClube(
            @PathVariable String leilaoId,
            @PathVariable String clubeId) {

        var historico = leilaoService.obterHistoricoLances(leilaoId, clubeId);
        return ResponseEntity.ok(historico);
    }

    @GetMapping("/{id}/resultado-final")
    public ResponseEntity<List<ResultadoLeilaoDTO>> verResultadoFinal(@PathVariable String id) {
        var resultado = leilaoService.obterResultadoFinal(id);

        if (resultado.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}/meu-status")
    public ResponseEntity<List<StatusLanceJogadorDTO>> verMeuStatus(@PathVariable String id,
                                                                    Authentication authentication) {
        String jogadorId = authentication.getName();
        var status = leilaoService.obterStatusDoJogador(id, jogadorId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/temporada/{temporadaId}")
    public ResponseEntity<List<Leilao>> listarPorTemporada(@PathVariable String temporadaId) {
        List<Leilao> leiloes = leilaoService.listarPorTemporada(temporadaId);
        return ResponseEntity.ok(leiloes);
    }

    @GetMapping("/temporada/{temporadaId}/existe")
    public ResponseEntity<Boolean> verificarSeExisteLeilao(@PathVariable String temporadaId) {
        boolean existe = leilaoService.existeLeilaoParaTemporada(temporadaId);
        return ResponseEntity.ok(existe);
    }

    @GetMapping("/{leilaoId}/meus-lances")
    public ResponseEntity<List<LanceDetalheDTO>> verMeusLancesParaEdicao(
            @PathVariable String leilaoId,
            Authentication authentication) {

        String jogadorId = "";
        jogadorId = authentication.getName();

        List<LanceDetalheDTO> lances = leilaoService.buscarLancesDoJogador(leilaoId, jogadorId);

        return ResponseEntity.ok(lances);
    }

    @GetMapping("/{leilaoId}/feed")
    public ResponseEntity<List<FeedItemDTO>> getFeedInicial(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.obterFeedInicial(leilaoId));
    }

    @GetMapping("/{leilaoId}/mais-disputados")
    public ResponseEntity<List<ClubeDisputadoDTO>> getMaisDisputados(@PathVariable String leilaoId) {
        return ResponseEntity.ok(leilaoService.obterTermometro(leilaoId));
    }

    @PostMapping("/lance/v2")
    public ResponseEntity<Void> realizarLanceV2(@RequestBody @Valid RealizarLanceDTO dto,
                                                Authentication authentication) {
        String jogadorId = authentication.getName();
        leilaoService.registrarLancesBlindado(jogadorId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{leilaoId}/meus-lances")
    public ResponseEntity<Void> resetarMeusLances(@PathVariable String leilaoId, Authentication authentication) {
        String jogadorId = authentication.getName();
        leilaoService.resetarLancesDoJogador(leilaoId, jogadorId);
        return ResponseEntity.noContent().build();
    }
}