package com.ddo.torneios.controller;

import com.ddo.torneios.dto.ChatRequestDTO;
import com.ddo.torneios.dto.SuporteDTO;
import com.ddo.torneios.service.SuporteVirtualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suporte")
public class SuporteController {

    @Autowired
    private SuporteVirtualService suporteService;

    @PostMapping("/chat")
    public ResponseEntity<SuporteDTO> conversar(@RequestBody ChatRequestDTO chatRequest) {

        if (chatRequest.novaPergunta() == null || chatRequest.novaPergunta().isBlank()) {
            return ResponseEntity.badRequest().body(new SuporteDTO(null, "Digite algo..."));
        }

        try {
            SuporteDTO resposta = suporteService.responderComContexto(chatRequest);
            return ResponseEntity.ok(resposta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SuporteDTO(chatRequest.novaPergunta(), "Erro ao processar conversa: " + e.getMessage()));
        }
    }
}