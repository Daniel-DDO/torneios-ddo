package com.ddo.torneios.controller;

import com.ddo.torneios.model.Conquista;
import com.ddo.torneios.model.Titulo;
import com.ddo.torneios.request.ConcederTituloLegadoRequest;
import com.ddo.torneios.request.ConcederTituloRequest;
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
    public ResponseEntity<?> concederTitulo(@RequestBody ConcederTituloRequest request) {
        try {
            Conquista conquistaGerada = tituloService.concederTituloAoJogador(
                    request.jogadorClubeId(),
                    request.idTitulo(),
                    request.edicao()
            );

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Título concedido com sucesso!",
                    "idConquista", conquistaGerada.getId(),
                    "imagemGerada", conquistaGerada.getImagem() != null ? conquistaGerada.getImagem() : "",
                    "nomeTitulo", conquistaGerada.getTitulo().getNome()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/conceder-legado")
    public ResponseEntity<?> concederTituloLegado(@RequestBody ConcederTituloLegadoRequest request) {
        try {
            Conquista conquistaGerada = tituloService.concederTituloLegado(
                    request.jogadorId(),
                    request.clubeId(),
                    request.idTitulo(),
                    request.edicao(),
                    request.data()
            );

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Título legado concedido com sucesso!",
                    "idConquista", conquistaGerada.getId(),
                    "imagemGerada", conquistaGerada.getImagem() != null ? conquistaGerada.getImagem() : "",
                    "nomeTitulo", conquistaGerada.getTitulo().getNome()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}