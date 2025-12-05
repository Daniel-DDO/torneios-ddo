package com.ddo.torneios.controller;

import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.request.JogadorRequest;
import com.ddo.torneios.service.JogadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jogador")
public class JogadorController {

    @Autowired
    private JogadorService jogadorService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarJogador(@RequestBody JogadorRequest jogador) {
        jogadorService.cadastrarJogador(jogador);
        return ResponseEntity.ok(jogador);
    }

    @GetMapping("/all")
    public ResponseEntity<PaginacaoDTO<JogadorDTO>> listarJogadores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "") String nomeFiltro
    ) {
        PaginacaoDTO<JogadorDTO> pagina = jogadorService.listarJogadores(nomeFiltro, page, size, sortBy, direction);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JogadorDTO> retornarJogador(@PathVariable String id) {
        return jogadorService.retornarJogador(id);
    }
}
