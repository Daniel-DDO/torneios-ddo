package com.ddo.torneios.controller;

import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.service.CompeticaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
