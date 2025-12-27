package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.service.BracketService;
import com.ddo.torneios.service.gerador.GeradorPartidasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bracket")
@RequiredArgsConstructor
public class BracketController {

    private final BracketService bracketService;
    private final GeradorPartidasService geradorService;

    @GetMapping("/{faseId}")
    public ResponseEntity<?> buscarBracket(@PathVariable String faseId) {
        return geradorService.buscarPorId(faseId)
                .map(fase -> {
                    Map<String, List<PartidaDTO>> bracket = bracketService.obterBracket(fase);
                    return ResponseEntity.ok(bracket);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}