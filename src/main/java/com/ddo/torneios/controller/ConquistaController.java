package com.ddo.torneios.controller;

import com.ddo.torneios.dto.ConquistaDashboardDTO;
import com.ddo.torneios.service.ConquistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/conquistas")
public class ConquistaController {

    @Autowired
    private ConquistaService conquistaService;

    @GetMapping("/destaque")
    public ResponseEntity<ConquistaDashboardDTO> getUltimaConquista() {
        return conquistaService.buscarDestaque()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/recentes")
    public ResponseEntity<List<ConquistaDashboardDTO>> getConquistasRecentes() {
        List<ConquistaDashboardDTO> recentes = conquistaService.buscarUltimasConquistas();
        return ResponseEntity.ok(recentes);
    }

    @GetMapping("/jogador/{id}")
    public ResponseEntity<List<ConquistaDashboardDTO>> getConquistasPorJogador(@PathVariable String id) {
        List<ConquistaDashboardDTO> conquistas = conquistaService.buscarPorJogador(id);
        return ResponseEntity.ok(conquistas);
    }

    @GetMapping("/clube/{id}")
    public ResponseEntity<List<ConquistaDashboardDTO>> getConquistasPorClube(@PathVariable String id) {
        List<ConquistaDashboardDTO> conquistas = conquistaService.buscarPorClube(id);
        return ResponseEntity.ok(conquistas);
    }

    @GetMapping("/titulo/{id}")
    public ResponseEntity<List<ConquistaDashboardDTO>> getHistoricoDoTitulo(@PathVariable String id) {
        List<ConquistaDashboardDTO> historico = conquistaService.buscarPorTitulo(id);
        return ResponseEntity.ok(historico);
    }
}