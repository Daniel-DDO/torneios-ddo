package com.ddo.torneios.controller;

import com.ddo.torneios.dto.NotificacaoDTO;
import com.ddo.torneios.request.NotificacaoRequest;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.service.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private JogadorRepository jogadorRepository;

    @PostMapping("/enviar-jogador")
    public ResponseEntity<Void> enviarParaJogador(@RequestBody NotificacaoRequest request) {
        Jogador jogador = jogadorRepository.findById(request.jogadorId())
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        notificacaoService.enviarParaJogador(
                jogador,
                request.titulo(),
                request.mensagem(),
                request.link(),
                request.tipo()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/enviar-todos")
    public ResponseEntity<Void> enviarParaTodos(@RequestBody NotificacaoRequest request) {
        notificacaoService.enviarParaTodos(
                request.titulo(),
                request.mensagem(),
                request.link()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<NotificacaoDTO>> exibirMinhasNotificacoes(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String jogadorId = principal.getName();

        List<NotificacaoDTO> notificacoes = notificacaoService.listarPorJogador(jogadorId);
        return ResponseEntity.ok(notificacoes);
    }

    @GetMapping("/admin/todas")
    public ResponseEntity<Page<NotificacaoDTO>> exibirTodas(
            @PageableDefault(sort = "dataCriacao", direction = Sort.Direction.DESC, size = 20) Pageable pageable
    ) {
        Page<NotificacaoDTO> page = notificacaoService.listarTodas(pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable String id) {
        notificacaoService.marcarComoLida(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/lidas")
    public ResponseEntity<String> apagarTodasMarcadasComoLida() {
        int count = notificacaoService.apagarTodasLidas();
        return ResponseEntity.ok("Removidas " + count + " notificações lidas.");
    }

    @DeleteMapping("/antigas")
    public ResponseEntity<String> apagarAntigasManual() {
        int count = notificacaoService.apagarMaisAntigasQue(13);
        return ResponseEntity.ok("Removidas " + count + " notificações antigas (+13 dias).");
    }

    @DeleteMapping("/geral")
    public ResponseEntity<Void> apagarTudo() {
        notificacaoService.apagarTudo();
        return ResponseEntity.noContent().build();
    }
}