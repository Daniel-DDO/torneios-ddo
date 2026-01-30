package com.ddo.torneios.controller;

import com.ddo.torneios.dto.DisputaClubeDTO;
import com.ddo.torneios.dto.RealizarLanceDTO;
import com.ddo.torneios.dto.ResultadoLeilaoDTO;
import com.ddo.torneios.dto.StatusLanceJogadorDTO;
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
        Leilao leilao = leilaoService.iniciarLeilao(request.temporadaId(), dataFim);
        return ResponseEntity.ok(leilao);
    }

    @PostMapping("/admin/{id}/finalizar")
    public ResponseEntity<String> finalizarLeilao(@PathVariable String id) {
        leilaoService.finalizarLeilao(id);
        return ResponseEntity.ok("Leilão finalizado e processado com sucesso. Vencedores definidos.");
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
}