package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.model.LigaClube;
import com.ddo.torneios.request.AtualizarValoresClubeRequest;
import com.ddo.torneios.request.ClubeRequest;
import com.ddo.torneios.request.MultiplicarValoresRequest;
import com.ddo.torneios.service.ClubeService;
import com.ddo.torneios.service.MercadoFinanceiroService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clube")
public class ClubeController {

    @Autowired
    private ClubeService clubeService;

    @Autowired
    private MercadoFinanceiroService mercadoFinanceiroService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarClube(@Valid @RequestBody ClubeRequest request) {
        clubeService.cadastrarClube(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/all")
    public ResponseEntity<PaginacaoDTO<Clube>> listarClubes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String nomeFiltro
    ) {
        PaginacaoDTO<Clube> pagina = clubeService.listarClubes(nomeFiltro, page, size, sortBy, direction);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Clube> retornarClube(@PathVariable String id) {
        return clubeService.retornarClube(id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Clube> atualizarClube(@PathVariable String id, @RequestBody ClubeRequest request) {
        Clube clubeAtualizado = clubeService.atualizarClube(id, request);
        return ResponseEntity.ok(clubeAtualizado);
    }

    @GetMapping("/buscar-autocomplete")
    public ResponseEntity<List<Clube>> buscarAutocomplete(@RequestParam String termo) {
        List<Clube> sugestoes = clubeService.buscarAutocomplete(termo);
        return ResponseEntity.ok(sugestoes);
    }

    @GetMapping("/selecoes")
    public PaginacaoDTO<ClubeResumoDTO> getSelecoes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarSomenteSelecoes(page, size);
    }

    @GetMapping("/clubes")
    public PaginacaoDTO<ClubeResumoDTO> getClubesExcetoSelecao(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarExcetoSelecoes(page, size);
    }

    @GetMapping("/liga/{liga}")
    public PaginacaoDTO<ClubeResumoDTO> getPorLiga(
            @PathVariable LigaClube liga,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return clubeService.listarPorLiga(liga, page, size);
    }

    @GetMapping("/estatisticas/contagem/{liga}")
    public ResponseEntity<Long> getContagemPorLiga(@PathVariable LigaClube liga) {
        return ResponseEntity.ok(clubeService.contarClubesPorLiga(liga));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable String id) {
        clubeService.alternarStatusAtivo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rankings/titulos")
    public List<Clube> getTopVencedores(@RequestParam(defaultValue = "5") int limit) {
        return clubeService.listarTopVencedores(limit);
    }

    @PostMapping("/cadastrar-lote")
    public ResponseEntity<Void> cadastrarEmLote(@RequestBody List<@Valid ClubeRequest> requests) {
        clubeService.cadastrarVariosClubes(requests);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/ligas")
    public ResponseEntity<LigaClube[]> getLigas() {
        return ResponseEntity.ok(clubeService.listarLigas());
    }

    @GetMapping("/leilao/todos")
    public ResponseEntity<Page<ClubeLeilaoDTO>> listarTodos(
            @PageableDefault(page = 0, size = 20, sort = "valorAvaliado", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(clubeService.listarTodos(pageable));
    }

    @GetMapping("/leilao/clubes")
    public ResponseEntity<Page<ClubeLeilaoDTO>> listarApenasClubes(
            @PageableDefault(page = 0, size = 20, sort = "valorAvaliado", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(clubeService.listarClubes(pageable));
    }

    @GetMapping("/leilao/selecoes")
    public ResponseEntity<Page<ClubeLeilaoDTO>> listarApenasSelecoes(
            @PageableDefault(page = 0, size = 20, sort = "valorAvaliado", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(clubeService.listarApenasSelecoes(pageable));
    }

    @PatchMapping("/{id}/valores")
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public ResponseEntity<Void> atualizarValores(
            @PathVariable String id,
            @RequestBody @Valid AtualizarValoresClubeRequest request) {

        clubeService.atualizarValoresClube(id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mercado/multiplicar-todos")
    public ResponseEntity<MultiplicacaoResultadoDTO> multiplicarValoresDeTodos(
            @Valid @RequestBody MultiplicarValoresRequest request) {
        return ResponseEntity.ok(clubeService.multiplicarValoresDeTodos(request.multiplicador()));
    }

    @PatchMapping("/{id}/mercado/multiplicar")
    public ResponseEntity<Void> multiplicarValorDoClube(
            @PathVariable String id,
            @Valid @RequestBody MultiplicarValoresRequest request) {
        clubeService.multiplicarValorDoClube(id, request.multiplicador());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mercado/status")
    public ResponseEntity<MercadoStatusDTO> consultarStatusMercado() {
        return ResponseEntity.ok(mercadoFinanceiroService.consultarStatus());
    }

    @PostMapping("/mercado/forcar-atualizacao")
    public ResponseEntity<MercadoStatusDTO> forcarAtualizacaoMercado() {
        mercadoFinanceiroService.forcarAtualizacaoAgora();
        return ResponseEntity.ok(mercadoFinanceiroService.consultarStatus());
    }
}