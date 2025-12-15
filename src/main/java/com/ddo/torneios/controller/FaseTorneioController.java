package com.ddo.torneios.controller;

import com.ddo.torneios.dto.FaseTorneioDTO;
import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.dto.RelatorioFaseDTO;
import com.ddo.torneios.dto.RodadaDTO;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.ZonaFase;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.request.FaseTorneioRequest;
import com.ddo.torneios.service.ClassificacaoService;
import com.ddo.torneios.service.ExportService;
import com.ddo.torneios.service.FaseTorneioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fase-torneio")
public class FaseTorneioController {

    @Autowired
    private FaseTorneioService faseTorneioService;

    @Autowired
    private ClassificacaoService classificacaoService;

    @Autowired
    private FaseTorneioRepository faseRepository;

    @Autowired
    private ExportService exportService;

    @PostMapping("/criar")
    public ResponseEntity<FaseTorneioDTO> criarFase(@RequestBody @Valid FaseTorneioRequest request) {
        FaseTorneioDTO novaFase = faseTorneioService.criarFase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaFase);
    }

    @GetMapping("/torneio/{torneioId}")
    public ResponseEntity<List<FaseTorneioDTO>> listarPorTorneio(@PathVariable String torneioId) {
        List<FaseTorneioDTO> fases = faseTorneioService.listarFasesDoTorneio(torneioId);
        return ResponseEntity.ok(fases);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarFase(@PathVariable String id) {
        faseTorneioService.deletarFase(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar-autocomplete")
    public ResponseEntity<List<FaseTorneioDTO>> buscarAutocomplete(@RequestParam String termo) {
        List<FaseTorneioDTO> fases = faseTorneioService.buscarAutocomplete(termo);
        return ResponseEntity.ok(fases);
    }

    @GetMapping("/{faseId}/tabela")
    public ResponseEntity<List<RodadaDTO>> getTabelaLiga(@PathVariable String faseId) {
        List<RodadaDTO> tabela = faseTorneioService.buscarTabelaLiga(faseId);
        return ResponseEntity.ok(tabela);
    }

    @GetMapping("/{faseId}/partidas")
    public ResponseEntity<List<PartidaDTO>> getPartidasFase(@PathVariable String faseId) {
        List<PartidaDTO> partidas = faseTorneioService.buscarPartidasMataMata(faseId);
        return ResponseEntity.ok(partidas);
    }

    @GetMapping("/{faseId}/jogador/{jogadorClubeId}/historico")
    public ResponseEntity<List<PartidaDTO>> getHistoricoJogador(
            @PathVariable String faseId,
            @PathVariable String jogadorClubeId) {

        List<PartidaDTO> historico = faseTorneioService.buscarHistoricoJogador(faseId, jogadorClubeId);
        return ResponseEntity.ok(historico);
    }

    @PostMapping("/{faseId}/zonas")
    public ResponseEntity<Void> atualizarZonas(
            @PathVariable String faseId,
            @RequestBody List<ZonaFase> novasZonas) {

        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase não encontrada"));

        fase.setZonas(novasZonas);
        faseRepository.save(fase);

        classificacaoService.recalcularETransmitir(fase);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{faseId}/dados-exportacao")
    public ResponseEntity<RelatorioFaseDTO> getDadosExportacao(@PathVariable String faseId) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new RuntimeException("Fase não encontrada"));

        RelatorioFaseDTO dados = exportService.prepararDadosExportacao(fase);
        return ResponseEntity.ok(dados);
    }
}