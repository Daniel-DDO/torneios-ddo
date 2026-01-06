package com.ddo.torneios.controller;

import com.ddo.torneios.model.Insignia;
import com.ddo.torneios.request.InsigniaRequest;
import com.ddo.torneios.service.InsigniaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/insignia")
public class InsigniaController {

    @Autowired
    private InsigniaService insigniaService;

    @GetMapping
    public ResponseEntity<List<Insignia>> listarTodas() {
        return ResponseEntity.ok(insigniaService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody InsigniaRequest request) {
        try {
            Insignia novaInsignia = insigniaService.criarInsignia(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaInsignia);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/conceder")
    public ResponseEntity<?> concederManual(@RequestBody ConcessaoManualDTO dto) {
        try {
            insigniaService.concederInsigniaManual(dto.jogadorId(), dto.nomeInsignia());
            return ResponseEntity.ok("Insígnia concedida com sucesso (se o jogador já não a possuía).");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    public record ConcessaoManualDTO(String jogadorId, String nomeInsignia) {}
}