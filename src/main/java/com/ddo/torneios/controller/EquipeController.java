package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.service.*;
import com.ddo.torneios.request.*;
import lombok.RequiredArgsConstructor;

import java.util.*;
import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/equipe")
@RequiredArgsConstructor
public class EquipeController {

    private static final long MAX_IMAGEM_BYTES = 2L * 1024 * 1024;

    private final EquipeService equipeService;
    private final EquipeConsultaService consulta;
    private final ImgBBService imgBBService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<EquipeResumoDTO> criar(@RequestBody @Valid CriarEquipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipeService.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<EquipeResumoDTO> editar(@PathVariable String id,
                                                  @RequestBody @Valid EditarEquipeRequest request) {
        return ResponseEntity.ok(equipeService.editar(id, request));
    }

    @PatchMapping("/{id}/capitao")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<Void> trocarCapitao(@PathVariable String id,
                                              @RequestBody @Valid TrocarCapitaoRequest request) {
        equipeService.trocarCapitao(id, request.novoCapitaoId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/membros")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<Void> adicionarMembros(@PathVariable String id,
                                                 @RequestBody @Valid AdicionarMembrosRequest request) {
        equipeService.adicionarMembros(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/membros/{participacaoId}")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<Void> removerMembro(@PathVariable String id, @PathVariable String participacaoId) {
        equipeService.removerMembro(id, participacaoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        equipeService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fase/{faseId}/validar")
    @PreAuthorize("hasAnyAuthority('PROPRIETARIO', 'DIRETOR')")
    public ResponseEntity<List<String>> validar(@PathVariable String faseId) {
        return ResponseEntity.ok(consulta.validarElenco(faseId));
    }

    // ---- leitura: pública ----
    @GetMapping("/torneio/{torneioId}")
    public ResponseEntity<List<EquipeResumoDTO>> listar(@PathVariable String torneioId) {
        return ResponseEntity.ok(consulta.listarPorTorneio(torneioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeResumoDTO> buscar(@PathVariable String id) {
        return ResponseEntity.ok(consulta.buscar(id));
    }

    @GetMapping("/fase/{faseId}/tabelao")
    public ResponseEntity<List<EquipeTabelaoDTO>> tabelao(@PathVariable String faseId) {
        return ResponseEntity.ok(consulta.tabelao(faseId));
    }

    @GetMapping("/{id}/fase/{faseId}")
    public ResponseEntity<EquipeTabelaoDTO> detalhe(@PathVariable String id, @PathVariable String faseId) {
        return ResponseEntity.ok(consulta.detalhe(faseId, id));
    }
}