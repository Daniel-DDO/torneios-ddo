package com.ddo.torneios.controller;

import com.ddo.torneios.dto.TituloResumoDTO;
import com.ddo.torneios.model.Conquista;
import com.ddo.torneios.model.Titulo;
import com.ddo.torneios.request.ConcederTituloColetivoRequest;
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

    @GetMapping("/ativos")
    public ResponseEntity<List<Titulo>> listarAtivos() {
        List<Titulo> titulosAtivos = tituloService.listarAtivos();
        return ResponseEntity.ok(titulosAtivos);
    }

    @GetMapping("/inativos")
    public ResponseEntity<List<Titulo>> listarInativos() {
        List<Titulo> titulosInativos = tituloService.listarInativos();
        return ResponseEntity.ok(titulosInativos);
    }

    @PostMapping("/conceder-coletivo")
    public ResponseEntity<?> concederTituloColetivo(@RequestBody ConcederTituloColetivoRequest request) {
        try {
            List<Conquista> conquistas = tituloService.concederTituloColetivo(request);

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Títulos concedidos com sucesso!",
                    "totalJogadoresPremiados", conquistas.size(),
                    "clubePremiado", request.getClubeId(),
                    "imagensGeradas", conquistas.stream()
                            .filter(c -> c.getImagem() != null)
                            .map(Conquista::getImagem)
                            .toList()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/buscar-autocomplete")
    public ResponseEntity<List<TituloResumoDTO>> autocomplete(@RequestParam String termo) {
        return ResponseEntity.ok(tituloService.buscarAutocomplete(termo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable String id) {
        try {
            Titulo titulo = tituloService.buscarPorId(id);
            return ResponseEntity.ok(titulo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}