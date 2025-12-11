package com.ddo.torneios.controller;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.service.CompeticaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/competicao")
public class CompeticaoController {

    @Autowired
    private CompeticaoService competicaoService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarCompeticao(@RequestBody Competicao competicao) {
        competicaoService.criarCompeticao(competicao);
        return ResponseEntity.ok(competicao);
    }

    @GetMapping("/all")
    public ResponseEntity<PaginacaoDTO<Competicao>> listarCompeticoes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String nomeFiltro
    ) {
        PaginacaoDTO<Competicao> pagina = competicaoService.listarCompeticoes(nomeFiltro, page, size, sortBy, direction);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/lista-simples")
    public ResponseEntity<List<Competicao>> listarTodasParaSelect() {
        List<Competicao> lista = competicaoService.listarTodasSemPaginacao();
        return ResponseEntity.ok(lista);
    }
}
