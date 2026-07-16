package com.ddo.torneios.controller;

import com.ddo.torneios.dto.JogadorClubeDTO;
import com.ddo.torneios.dto.JogadorClubeInscritoDTO;
import com.ddo.torneios.dto.SorteioResultadoDTO;
import com.ddo.torneios.dto.SubstituicaoDTO;
import com.ddo.torneios.request.ConfirmacaoSorteioRequest;
import com.ddo.torneios.request.JogadorClubeRequest;
import com.ddo.torneios.request.SorteioRequest;
import com.ddo.torneios.service.JogadorClubeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inscricao")
public class JogadorClubeController {

    @Autowired
    private JogadorClubeService jogadorClubeService;

    @PostMapping("/inscrever")
    public ResponseEntity<JogadorClubeDTO> realizarInscricao(@RequestBody @Valid JogadorClubeRequest request) {
        JogadorClubeDTO dto = jogadorClubeService.inscreverJogador(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/temporada/{temporadaId}")
    public ResponseEntity<List<JogadorClubeDTO>> listarInscritos(@PathVariable String temporadaId) {
        List<JogadorClubeDTO> inscritos = jogadorClubeService.listarInscritosPorTemporada(temporadaId);
        return ResponseEntity.ok(inscritos);
    }

    @GetMapping("/temporada/{temporadaId}/resumo")
    public ResponseEntity<List<JogadorClubeInscritoDTO>> listarInscritosResumo(@PathVariable String temporadaId) {
        return ResponseEntity.ok(jogadorClubeService.listarInscritosResumoPorTemporada(temporadaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarInscricao(@PathVariable String id) {
        jogadorClubeService.removerInscricao(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/torneio/{torneioId}")
    public ResponseEntity<List<JogadorClubeDTO>> listarInscritosPorTorneio(@PathVariable String torneioId) {
        List<JogadorClubeDTO> inscritos = jogadorClubeService.listarInscritosPorTorneio(torneioId);
        return ResponseEntity.ok(inscritos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<JogadorClubeDTO>> listarTodos() {
        List<JogadorClubeDTO> lista = jogadorClubeService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/buscar-autocomplete/jogador")
    public ResponseEntity<List<JogadorClubeDTO>> buscarPorNomeJogador(@RequestParam String termo) {
        List<JogadorClubeDTO> resultados = jogadorClubeService.buscarAutocompletePorJogador(termo);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/buscar-autocomplete/clube")
    public ResponseEntity<List<JogadorClubeDTO>> buscarPorNomeClube(@RequestParam String termo) {
        List<JogadorClubeDTO> resultados = jogadorClubeService.buscarAutocompletePorClube(termo);
        return ResponseEntity.ok(resultados);
    }

    @PutMapping("/substituir-jogador")
    public ResponseEntity<Void> substituirJogador(@RequestBody SubstituicaoDTO dto) {
        jogadorClubeService.substituirJogador(dto.idInscricaoAntiga(), dto.idNovoAlvo());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/trocar-clube")
    public ResponseEntity<Void> trocarClube(@RequestBody SubstituicaoDTO dto) {
        jogadorClubeService.trocarClube(dto.idInscricaoAntiga(), dto.idNovoAlvo());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar-autocomplete/temporada")
    public ResponseEntity<List<JogadorClubeDTO>> buscarPorTemporada(
            @RequestParam String termo,
            @RequestParam String temporadaId) {

        List<JogadorClubeDTO> resultados = jogadorClubeService.buscarAutocompleteNaTemporada(termo, temporadaId);
        return ResponseEntity.ok(resultados);
    }

    @PostMapping("/sorteio")
    public ResponseEntity<List<JogadorClubeDTO>> realizarSorteio(@RequestBody @Valid SorteioRequest request) {
        List<JogadorClubeDTO> resultado = jogadorClubeService.realizarSorteio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/sorteio/simular")
    public ResponseEntity<List<SorteioResultadoDTO>> simularSorteio(@RequestBody @Valid SorteioRequest request) {
        List<SorteioResultadoDTO> resultado = jogadorClubeService.simularSorteio(request);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/sorteio/confirmar")
    public ResponseEntity<Void> confirmarSorteio(@RequestBody @Valid ConfirmacaoSorteioRequest request) {
        jogadorClubeService.confirmarInscricoesEmLote(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}