package com.ddo.torneios.controller;

import com.ddo.torneios.model.Titulo;
import com.ddo.torneios.request.TituloRequest;
import com.ddo.torneios.service.TituloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/titulos")
public class TituloController {

    @Autowired
    private TituloService tituloService;

    @GetMapping
    public ResponseEntity<List<Titulo>> listar() {
        return ResponseEntity.ok(tituloService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody TituloRequest request) {
        try {
            Titulo criado = tituloService.criarTitulo(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(criado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/lote")
    public ResponseEntity<List<Titulo>> criarEmLote(@RequestBody List<TituloRequest> requests) {
        List<Titulo> criados = tituloService.criarTitulosEmLote(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(criados);
    }

    @PostMapping("/conceder")
    public ResponseEntity<?> concederTitulo(@RequestBody Map<String, String> payload) {
        try {
            String jogadorId = payload.get("jogadorId");
            String nomeTitulo = payload.get("nomeTitulo");
            String edicao = payload.get("edicao");

            tituloService.concederTituloAoJogador(jogadorId, nomeTitulo, edicao);

            return ResponseEntity.ok(Map.of("mensagem", "Título concedido com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}