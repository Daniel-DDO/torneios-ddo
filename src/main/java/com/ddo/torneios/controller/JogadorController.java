package com.ddo.torneios.controller;

import com.ddo.torneios.request.JogadorRequest;
import com.ddo.torneios.service.JogadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
