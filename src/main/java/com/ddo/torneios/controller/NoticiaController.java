package com.ddo.torneios.controller;

import com.ddo.torneios.model.Noticia;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.service.NoticiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/noticias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoticiaController {

    private final NoticiaService noticiaService;
    private final PartidaRepository partidaRepository;

    @GetMapping
    public ResponseEntity<List<Noticia>> listarUltimas() {
        return ResponseEntity.ok(noticiaService.listarUltimas());
    }

    @PostMapping("/gerar/{idPartida}")
    public ResponseEntity<String> forcarGeracao(@PathVariable String idPartida) {
        return partidaRepository.findById(idPartida)
                .map(partida -> {
                    noticiaService.gerarNoticiaSeRelevante(partida);
                    return ResponseEntity.ok("Solicitação enviada para o serviço de IA.");
                })
                .orElse(ResponseEntity.notFound().build());
    }

}