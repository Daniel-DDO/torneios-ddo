package com.ddo.torneios.controller;

import com.ddo.torneios.model.Conquista;
import com.ddo.torneios.repository.ConquistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conquistas")
public class ConquistaController {

    @Autowired
    private ConquistaRepository conquistaRepository;

    @GetMapping("/{id}")
    public ResponseEntity<Conquista> buscarPorId(@PathVariable String id) {
        return conquistaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<Void> redirecionarParaImagem(@PathVariable String id) {
        return conquistaRepository.findById(id)
                .filter(c -> c.getImagem() != null)
                .map(c -> ResponseEntity.status(302).header("Location", c.getImagem()).<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }
}