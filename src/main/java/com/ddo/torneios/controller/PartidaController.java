package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.service.ClassificacaoService;
import com.ddo.torneios.service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partida")
public class PartidaController {

    @Autowired
    private ClassificacaoService classificacaoService;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private PartidaService partidaService;

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

    @GetMapping("/fase/{faseId}")
    public ResponseEntity<List<PartidaDTO>> listarPorFase(@PathVariable String faseId) {
        return ResponseEntity.ok(partidaService.listarPorFase(faseId));
    }

    @GetMapping("/rodada/{rodadaId}")
    public ResponseEntity<List<PartidaDTO>> listarPorRodada(@PathVariable String rodadaId) {
        return ResponseEntity.ok(partidaService.listarPorRodada(rodadaId));
    }

    @GetMapping("/jogador/{jogadorId}/feitas")
    public ResponseEntity<List<PartidaDTO>> minhasPartidasFeitas(@PathVariable String jogadorId) {
        return ResponseEntity.ok(partidaService.minhasPartidasFeitas(jogadorId));
    }

    @GetMapping("/jogador/{jogadorId}/pendentes")
    public ResponseEntity<List<PartidaDTO>> minhasPartidasParaFazer(@PathVariable String jogadorId) {
        return ResponseEntity.ok(partidaService.minhasPartidasParaFazer(jogadorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartidaDTO> buscarPorId(@PathVariable String id) {
        return partidaRepository.findById(id)
                .map(PartidaDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<PartidaDTO>> buscarAutocomplete(@RequestParam String termo) {
        return ResponseEntity.ok(partidaService.buscarAutocomplete(termo));
    }
}