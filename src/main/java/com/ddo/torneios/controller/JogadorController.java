package com.ddo.torneios.controller;

import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.dto.LoginResponseDTO;
import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.request.*;
import com.ddo.torneios.service.JogadorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequest request) {
        LoginResponseDTO response = jogadorService.logarJogador(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/gerar-codigo")
    public ResponseEntity<String> gerarCodigo(@RequestBody @Valid GerarCodigoRequest request) {
        String codigo = jogadorService.gerarCodigoReivindicacao(request);
        return ResponseEntity.ok(codigo);
    }

    @PostMapping("/reivindicar")
    public ResponseEntity<Void> reivindicar(@RequestBody @Valid ReivindicarContaRequest request) {
        jogadorService.reivindicarConta(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pin")
    public ResponseEntity<Integer> consultarPin(
            @RequestParam String adminId,
            @RequestParam String jogadorId) {
        Integer pin = jogadorService.consultarPinJogador(adminId, jogadorId);
        return ResponseEntity.ok(pin);
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<Void> recuperarSenha(@RequestBody @Valid RecuperarSenhaRequest request) {
        jogadorService.recuperarSenhaComPin(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/gerar-pins-legado")
    public ResponseEntity<String> gerarPins() {
        String resultado = jogadorService.gerarPinsParaJogadoresLegados();
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/reivindicar-direto")
    public ResponseEntity<Void> reivindicarDireto(@RequestBody ReivindicarDiretoRequest request) {
        jogadorService.reivindicarContaDiretamente(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/buscar-autocomplete")
    public ResponseEntity<List<Map<String, String>>> buscarJogadoresAutocomplete(@RequestParam String termo) {
        if (termo == null || termo.length() < 3) {
            return ResponseEntity.badRequest().build();
        }

        List<Jogador> jogadores = jogadorService.findByDiscordContainingIgnoreCase(termo);

        List<Map<String, String>> resultado = jogadores.stream()
                .limit(5)
                .map(j -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", String.valueOf(j.getId()));
                    map.put("discord", j.getDiscord());
                    map.put("nome", j.getNome());
                    map.put("imagem", j.getImagem());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/jogadores")
    public ResponseEntity<Page<Jogador>> listarJogadoresPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nome").ascending());
        Page<Jogador> jogadores = jogadorService.listarTodosPaginado(pageRequest);
        return ResponseEntity.ok(jogadores);
    }

    @PatchMapping("/perfil")
    public ResponseEntity<Jogador> editarPerfil(@RequestBody JogadorEditarRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String idJogadorLogado = authentication.getName();
        Jogador jogadorAtualizado = jogadorService.editarPerfilLogado(idJogadorLogado, request);

        return ResponseEntity.ok(jogadorAtualizado);
    }

    @PatchMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Jogador> atualizarFotoPerfil(@RequestParam("file") MultipartFile file) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String idJogador = authentication.getName();

        Jogador jogadorAtualizado = jogadorService.atualizarFotoPerfil(idJogador, file);

        return ResponseEntity.ok(jogadorAtualizado);
    }
}
