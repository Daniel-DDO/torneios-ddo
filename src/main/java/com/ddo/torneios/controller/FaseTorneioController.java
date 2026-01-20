package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.request.FaseTorneioRequest;
import com.ddo.torneios.service.ClassificacaoService;
import com.ddo.torneios.service.ExportService;
import com.ddo.torneios.service.FaseTorneioService;
import com.ddo.torneios.service.TransicaoFaseService;
import com.ddo.torneios.service.gerador.GeradorCopaLigaStrategy;
import com.ddo.torneios.service.gerador.GeradorPartidasService;
import jakarta.validation.Valid;
import lombok.Data;
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

    @Autowired
    private GeradorPartidasService geradorService;

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;

    @Autowired
    private TransicaoFaseService transicaoFaseService;

    @PostMapping("/criar")
    public ResponseEntity<FaseTorneioDTO> criarFase(@RequestBody @Valid FaseTorneioRequest request) {
        FaseTorneioDTO novaFase = faseTorneioService.criarFase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaFase);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FaseTorneioDTO> buscarPorId(@PathVariable String id) {
        FaseTorneioDTO fase = faseTorneioService.buscarPorId(id);
        return ResponseEntity.ok(fase);
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

    @PatchMapping("/{faseId}/estadio-final")
    public ResponseEntity<Void> atualizarEstadioFinal(
            @PathVariable String faseId,
            @RequestBody EstadioUpdateDTO dto) {

        if (dto.novoEstadio() == null || dto.novoEstadio().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        geradorService.atualizarEstadioFinalManualmente(faseId, dto.novoEstadio());

        return ResponseEntity.ok().build();
    }

    @Autowired
    private TransicaoFaseService transicaoService;

    @PostMapping("/{faseId}/gerar-mata-mata")
    public ResponseEntity<String> gerarMataMata(@PathVariable String faseId) {
        try {
            transicaoService.inicializarFaseMataMata(faseId);
            return ResponseEntity.ok("Processo de geração de mata-mata iniciado com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro interno ao gerar mata-mata: " + e.getMessage());
        }
    }

    @PostMapping("/{faseId}/confirmar-mata-mata-manual")
    public ResponseEntity<String> confirmarMataMataManual(@PathVariable String faseId) {
        try {
            transicaoService.confirmarMataMataManual(faseId);
            return ResponseEntity.ok("Partidas geradas com sucesso baseadas na configuração manual dos potes!");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao confirmar mata-mata manual: " + e.getMessage());
        }
    }

    @GetMapping("/{faseId}/previa-classificados")
    public ResponseEntity<PreviaClassificadosDTO> getPreviaClassificados(@PathVariable String faseId) {
        return ResponseEntity.ok(transicaoService.obterPreviaClassificados(faseId));
    }

    @Data
    public static class GeracaoPartidasDTO {
        private AlgoritmoGeracaoMataMata algoritmoMataMata;
        private AlgoritmoGeracaoLiga algoritmoLiga;
    }

    /**
     * Endpoint UNIVERSAL de geração.
     * Serve para: Sorteio Total, Dirigido, Copa Real, Copa Liga, Pontos Corridos, etc.
     * O Service cuida de limpar dados antigos e chamar a factory correta.
     */
    @PostMapping("/{faseId}/gerar")
    public ResponseEntity<?> gerarPartidas(
            @PathVariable String faseId,
            @RequestBody(required = false) GeracaoPartidasDTO params) {

        try {
            AlgoritmoGeracaoMataMata algMata = params != null ? params.getAlgoritmoMataMata() : null;
            AlgoritmoGeracaoLiga algLiga = params != null ? params.getAlgoritmoLiga() : null;

            geradorService.gerarEstruturaFase(faseId, algMata, algLiga);

            return ResponseEntity.ok("Partidas geradas com sucesso!");

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro interno ao gerar partidas: " + e.getMessage());
        }
    }

    @PostMapping("/{faseId}/copa-liga/importar-eliminados")
    public ResponseEntity<?> importarEliminadosCopaLiga(
            @PathVariable String faseId,
            @RequestBody List<String> idsParticipacoesLigaReal) {

        try {
            if (idsParticipacoesLigaReal == null || idsParticipacoesLigaReal.size() != 4) {
                return ResponseEntity.badRequest().body("É necessário enviar exatamente 4 IDs de eliminados.");
            }

            List<ParticipacaoFase> eliminados = participacaoRepository.findAllById(idsParticipacoesLigaReal);

            if (eliminados.size() != 4) {
                return ResponseEntity.badRequest().body("Alguns IDs fornecidos não foram encontrados no banco.");
            }

            transicaoFaseService.distribuirEliminadosCopaLiga(faseId, eliminados);

            return ResponseEntity.ok("Eliminados importados e distribuídos nas Quartas de Final com sucesso!");

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao importar eliminados: " + e.getMessage());
        }
    }
}