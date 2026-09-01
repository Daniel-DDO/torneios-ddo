package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.ReportPartida;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.ReportPartidaRepository;
import com.ddo.torneios.request.AnularPartidaRequest;
import com.ddo.torneios.request.DefinirParticipanteRequest;
import com.ddo.torneios.request.RelatoProblemaRequest;
import com.ddo.torneios.service.ClassificacaoService;
import com.ddo.torneios.service.JuizVirtualService;
import com.ddo.torneios.service.PartidaService;
import com.ddo.torneios.service.ProbabilidadeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/partida")
public class PartidaController {

    @Autowired
    private ClassificacaoService classificacaoService;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private PartidaService partidaService;

    @Autowired
    private JuizVirtualService juizVirtualService;

    @Autowired
    private ReportPartidaRepository reportPartidaRepository;

    @Autowired
    private ProbabilidadeService probabilidadeService;

    @PostMapping("/registrar-resultado")
    public ResponseEntity<String> registrarResultado(@RequestBody PartidaDTO dto) {
        try {
            if (dto.id() == null) {
                return ResponseEntity.badRequest().body("ID da partida é obrigatório.");
            }

            classificacaoService.registrarResultado(dto);
            return ResponseEntity.ok("Resultado registrado e coeficientes calculados com sucesso!");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erro ao registrar resultado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno no servidor: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/desfazer-resultado")
    public ResponseEntity<String> desfazerResultado(@PathVariable String id) {
        try {
            classificacaoService.desfazerResultado(id);
            return ResponseEntity.ok("Resultado desfeito, pontuações revertidas e economia estornada com sucesso!");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Não foi possível desfazer o resultado: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro interno ao tentar desfazer resultado: " + e.getMessage());
        }
    }

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<List<PartidaDTO>> listarPorFase(@PathVariable String faseId) {
        return ResponseEntity.ok(partidaService.listarPorFase(faseId));
    }

    @GetMapping("/rodada/{rodadaId}")
    public ResponseEntity<List<PartidaDTO>> listarPorRodada(@PathVariable String rodadaId) {
        return ResponseEntity.ok(partidaService.listarPorRodada(rodadaId));
    }

    @GetMapping("/jogador/{jogadorId}/feitas")
    public ResponseEntity<PaginacaoDTO<PartidaHistoricoDTO>> minhasPartidasFeitas(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(partidaService.minhasPartidasFeitas(jogadorId, pagina, tamanho));
    }

    @GetMapping("/jogador/{jogadorId}/pendentes")
    public ResponseEntity<PaginacaoDTO<PartidaHistoricoDTO>> minhasPartidasPendentes(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(partidaService.minhasPartidasParaFazer(jogadorId, pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaDTO> buscarPorId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(partidaService.buscarPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<PartidaDTO>> buscarAutocomplete(@RequestParam String termo) {
        return ResponseEntity.ok(partidaService.buscarAutocomplete(termo));
    }

    @PostMapping("/{partidaId}/analisar-problema")
    public ResponseEntity<ReportPartidaDTO> analisarProblema(
            @PathVariable String partidaId,
            @RequestBody RelatoProblemaRequest request) {

        Optional<ReportPartida> reportExistente = reportPartidaRepository.findByPartida_Id(partidaId);

        if (reportExistente.isPresent()) {
            return ResponseEntity.ok(new ReportPartidaDTO(reportExistente.get()));
        }

        DadosPartidaDTO dadosDTO = DadosPartidaDTO.builder()
                .nomeMandante(request.nomeMandante())
                .timeMandante(request.timeMandante())
                .nomeVisitante(request.nomeVisitante())
                .timeVisitante(request.timeVisitante())
                .relatoOcorrido(request.relato())
                .build();

        ReportPartidaDTO novoReport = juizVirtualService.analisarDisputa(partidaId, dadosDTO);

        return ResponseEntity.ok(novoReport);
    }

    @PostMapping("/{partidaId}/reanalisar")
    public ResponseEntity<ReportPartidaDTO> forcarReanalise(
            @PathVariable String partidaId,
            @RequestBody RelatoProblemaRequest request) {

        reportPartidaRepository.findByPartida_Id(partidaId)
                .ifPresent(reportPartidaRepository::delete);

        DadosPartidaDTO dadosDTO = DadosPartidaDTO.builder()
                .nomeMandante(request.nomeMandante())
                .timeMandante(request.timeMandante())
                .nomeVisitante(request.nomeVisitante())
                .timeVisitante(request.timeVisitante())
                .relatoOcorrido(request.relato())
                .build();

        ReportPartidaDTO novoReport = juizVirtualService.analisarDisputa(partidaId, dadosDTO);
        return ResponseEntity.ok(novoReport);
    }

    @GetMapping("/{partidaId}/resultado")
    public ResponseEntity<ReportPartida> consultarResultado(@PathVariable String partidaId) {
        return reportPartidaRepository.findByPartida_Id(partidaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/partidas/torneio/{torneioId}")
    public ResponseEntity<List<PartidaDTO>> getPartidasPorTorneio(
            @PathVariable String id,
            @PathVariable String torneioId) {

        List<PartidaDTO> partidas = partidaService.minhasPartidasPorTorneio(id, torneioId);
        return ResponseEntity.ok(partidas);
    }

    @GetMapping("/{id}/partidas/fase/{faseId}")
    public ResponseEntity<List<PartidaDTO>> getPartidasPorFase(
            @PathVariable String id,
            @PathVariable String faseId) {

        List<PartidaDTO> partidas = partidaService.minhasPartidasPorFase(id, faseId);
        return ResponseEntity.ok(partidas);
    }

    @GetMapping("/{id}/probabilidade")
    public ResponseEntity<ProbabilidadePartidaDTO> getProbabilidadePartida(@PathVariable String id) {
        PartidaProbabilidadeDTO partida = partidaRepository.buscarPartidaParaProbabilidade(id)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada: " + id));

        ProbabilidadePartidaDTO probabilidade = probabilidadeService.calcularProbabilidade(partida);

        return ResponseEntity.ok(probabilidade);
    }

    @PatchMapping("/partidas/{id}/participante")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public ResponseEntity<Void> definirParticipante(
            @PathVariable String id,
            @RequestBody DefinirParticipanteRequest request) {
        partidaService.definirParticipante(id, request.participacaoFaseId(), request.lado());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jogador/{jogadorId}/anuladas")
    public ResponseEntity<PaginacaoDTO<PartidaHistoricoDTO>> partidasAnuladas(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(partidaService.minhasPartidasAnuladas(jogadorId, pagina, tamanho));
    }

    @GetMapping("/jogador/{jogadorId}/realizadas")
    public ResponseEntity<PaginacaoDTO<PartidaHistoricoDTO>> partidasRealizadas(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(partidaService.minhasPartidasFeitas(jogadorId, pagina, tamanho));
    }

    @GetMapping("/jogador/{jogadorId}/a-fazer")
    public ResponseEntity<PaginacaoDTO<PartidaHistoricoDTO>> partidasAFazer(
            @PathVariable String jogadorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(partidaService.minhasPartidasParaFazer(jogadorId, pagina, tamanho));
    }

    @GetMapping("/ranking/wo")
    @PreAuthorize("hasAnyRole('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<List<TopJogadorWoDTO>> topJogadoresWo(@RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(partidaService.topJogadoresDerrotasWo(limite));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public ResponseEntity<PartidaDTO> anular(@PathVariable String id, @RequestBody AnularPartidaRequest request) {
        return ResponseEntity.ok(partidaService.anularPartida(id, request.motivo()));
    }

    @PostMapping("/{id}/desanular")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public ResponseEntity<PartidaDTO> desanular(@PathVariable String id) {
        return ResponseEntity.ok(partidaService.desanularPartida(id));
    }
}