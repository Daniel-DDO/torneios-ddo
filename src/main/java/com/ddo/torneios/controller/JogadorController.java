package com.ddo.torneios.controller;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.model.StatusJogador;
import com.ddo.torneios.request.*;
import com.ddo.torneios.service.JogadorService;
import com.ddo.torneios.service.PlanilhaJogadorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jogador")
public class JogadorController {

    @Autowired
    private JogadorService jogadorService;

    @Autowired
    private PlanilhaJogadorService planilhaJogadorService;

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
    public ResponseEntity<List<JogadorResumo>> buscarJogadoresAutocomplete(@RequestParam String termo) {
        if (termo == null || termo.length() < 3) {
            return ResponseEntity.badRequest().build();
        }

        List<JogadorResumo> resultado = jogadorService.buscarAutocomplete(termo);
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

    @PatchMapping(value = "/uploadfoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> atualizarFotoPerfil(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String idJogador = authentication.getName();
        jogadorService.atualizarFotoPerfil(idJogador, file);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/avatarId")
    public ResponseEntity<JogadorDTO> atualizarFotoPorAvatarId(@RequestBody Map<String, String> payload) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String idJogador = authentication.getName();

        String avatarId = payload.get("avatarId");

        Jogador jogadorAtualizado = jogadorService.atualizarFotoPorAvatarId(idJogador, avatarId);

        JogadorDTO responseDTO = new JogadorDTO(jogadorAtualizado);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> removerAvatar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String idJogador = authentication.getName();

        jogadorService.removerAvatar(idJogador);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/credenciais")
    public ResponseEntity<Void> alterarCredenciais(
            @RequestBody @Valid AlterarCredenciaisRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        jogadorService.alterarCredenciais(userDetails.getUsername(), request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/todos")
    public PaginacaoDTO<JogadorDTO> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return jogadorService.listarJogadoresDinamico(page, size);
    }

    @GetMapping("/estatisticas/ativos")
    public ResponseEntity<Long> getTotalAtivos() {
        return ResponseEntity.ok(jogadorService.contarContasReivindicadas());
    }

    @PatchMapping("/{id}/cargo")
    public ResponseEntity<JogadorDTO> updateCargo(
            @PathVariable String id,
            @RequestParam Cargo novoCargo) {
        return ResponseEntity.ok(jogadorService.alterarCargo(id, novoCargo));
    }

    @GetMapping("/filtro/cargo")
    public PaginacaoDTO<JogadorDTO> getPorCargo(
            @RequestParam Cargo cargo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return jogadorService.listarPorCargo(cargo, page, size);
    }

    @GetMapping("/busca-rapida")
    public ResponseEntity<List<JogadorDTO>> buscarAutocomplete(@RequestParam String termo) {
        var resultados = jogadorService.buscarJogadorAutocomplete(termo);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/search")
    public ResponseEntity<List<JogadorResumoDTO>> searchJogadores(@RequestParam String termo) {
        return ResponseEntity.ok(jogadorService.buscarJogadoresParaSelect(termo));
    }

    @GetMapping("/by-coeficiente")
    public ResponseEntity<List<JogadorResumoDTO>> retornarTodosJogadoresMelhorCoeficiente() {
        return ResponseEntity.ok(jogadorService.retornarTodosJogadoresMelhorCoeficiente());
    }

    @GetMapping("/by-coeficiente-10")
    public ResponseEntity<List<JogadorResumoDTO>> retornarTop10JogadoresMelhorCoeficiente() {
        return ResponseEntity.ok(jogadorService.retornarTop10JogadoresMelhorCoeficiente());
    }

    @GetMapping("/{id}/historia")
    public ResponseEntity<JogadorHistoriaDTO> obterHistoriaJogador(@PathVariable String id) {
        return ResponseEntity.ok(jogadorService.obterResumoHistoria(id));
    }

    @GetMapping("/{jogadorId}/patos")
    public ResponseEntity<List<RivalidadeDTO>> buscarTop3Patos(@PathVariable String jogadorId) {
        return ResponseEntity.ok(jogadorService.buscarTop3Patos(jogadorId));
    }

    @GetMapping("/{jogadorId}/carrascos")
    public ResponseEntity<List<RivalidadeDTO>> buscarTop3Carrascos(@PathVariable String jogadorId) {
        return ResponseEntity.ok(jogadorService.buscarTop3Carrascos(jogadorId));
    }

    @PatchMapping("/{id}/saldo")
    public ResponseEntity<BigDecimal> atualizarSaldo(
            @PathVariable String id,
            @RequestBody @Valid MovimentacaoSaldoDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String responsavel = userDetails != null ? userDetails.getUsername() : "SISTEMA";

        BigDecimal novoSaldo = jogadorService.atualizarSaldo(id, dto, responsavel);
        return ResponseEntity.ok(novoSaldo);
    }

    @GetMapping("/{id}/momento")
    public ResponseEntity<List<String>> obterMomentoAtual(@PathVariable String id) {
        return ResponseEntity.ok(jogadorService.obterMomentoAtual(id));
    }

    @GetMapping("/{id}/transacoes")
    public ResponseEntity<Page<TransacaoResponseDTO>> getHistoricoFinanceiro(
            @PathVariable String id,
            @PageableDefault(size = 10, sort = "dataHora", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransacaoResponseDTO> transacoes = jogadorService.listarTransacoesDoJogador(id, pageable);
        return ResponseEntity.ok(transacoes);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> atualizarStatus(
            @PathVariable String id,
            @RequestBody @Valid AtualizarStatusRequest request) {

        jogadorService.alterarStatusJogador(id, request.getStatus());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ranking-financeiro")
    public ResponseEntity<Page<JogadorRankingDTO>> getRankingFinanceiro(
            @PageableDefault(size = 20, sort = "saldoVirtual", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(jogadorService.listarRankingFinanceiro(pageable));
    }

    @GetMapping("/comparar")
    public ResponseEntity<ComparacaoJogadoresDTO> comparar(
            @RequestParam String id1,
            @RequestParam String id2) {
        return ResponseEntity.ok(jogadorService.compararJogadores(id1, id2));
    }

    @GetMapping("/{id}/resumo")
    public ResponseEntity<JogadorResumoDTO> buscarJogadorResumo(@PathVariable String id) {
        return ResponseEntity.ok(jogadorService.buscarResumoPorId(id));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PatchMapping("/{id}/discord")
    public ResponseEntity<JogadorDTO> trocarDiscord(
            @PathVariable String id,
            @RequestBody @Valid TrocarDiscordRequest request) {
        return ResponseEntity.ok(jogadorService.trocarDiscord(id, request.novoDiscord()));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PatchMapping("/{id}/email-admin")
    public ResponseEntity<JogadorDTO> atualizarEmailAdmin(
            @PathVariable String id,
            @RequestBody @Valid AtualizarEmailAdminRequest request) {
        return ResponseEntity.ok(jogadorService.atualizarEmailAdmin(id, request.novoEmail()));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PatchMapping("/{id}/saldo/zerar")
    public ResponseEntity<BigDecimal> zerarSaldoJogador(
            @PathVariable String id,
            @RequestBody(required = false) ZerarSaldoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String motivo = request != null ? request.motivo() : null;
        String responsavel = userDetails != null ? userDetails.getUsername() : "SISTEMA";

        BigDecimal novoSaldo = jogadorService.zerarSaldoJogador(id, motivo, responsavel);
        return ResponseEntity.ok(novoSaldo);
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PostMapping("/saldo/zerar-todos")
    public ResponseEntity<String> zerarSaldoDeTodos(
            @RequestBody(required = false) ZerarSaldoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String motivo = request != null ? request.motivo() : null;
        String responsavel = userDetails != null ? userDetails.getUsername() : "SISTEMA";

        int afetados = jogadorService.zerarSaldoDeTodosOsJogadores(motivo, responsavel);
        return ResponseEntity.ok(afetados + " jogadores tiveram o saldo zerado.");
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PostMapping("/saldo/distribuir")
    public ResponseEntity<String> distribuirSaldoParaTodos(
            @RequestBody @Valid DistribuirSaldoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String responsavel = userDetails != null ? userDetails.getUsername() : "SISTEMA";

        int afetados = jogadorService.distribuirSaldoParaTodos(request.valor(), request.motivo(), responsavel);
        return ResponseEntity.ok(afetados + " jogadores receberam o saldo.");
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PatchMapping("/{id}/resetar-senha")
    public ResponseEntity<Void> resetarSenhaAdmin(
            @PathVariable String id,
            @RequestBody @Valid ResetarSenhaRequest request) {
        jogadorService.resetarSenhaAdmin(id, request.novaSenha());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PatchMapping("/{id}/resetar-pin")
    public ResponseEntity<Integer> resetarPinAdmin(@PathVariable String id) {
        return ResponseEntity.ok(jogadorService.resetarPinAdmin(id));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @DeleteMapping("/{id}/deletar")
    public ResponseEntity<Void> deletarJogador(@PathVariable String id) {
        jogadorService.deletarJogador(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PostMapping("/mesclar-contas")
    public ResponseEntity<JogadorDTO> mesclarContas(@RequestBody @Valid MesclarContasRequest request) {
        Jogador jogadorMesclado = jogadorService.mesclarContas(request.idPrincipal(), request.idsAntigos());
        return ResponseEntity.ok(new JogadorDTO(jogadorMesclado));
    }

    @PreAuthorize("hasAuthority('PROPRIETARIO')")
    @PutMapping("/{id}")
    public ResponseEntity<JogadorDTO> editarJogadorAdmin(
            @PathVariable String id,
            @RequestBody JogadorEditarRequest request) {
        return ResponseEntity.ok(jogadorService.editarJogadorAdmin(id, request));
    }

    @GetMapping("/{id}/planilha")
    public ResponseEntity<byte[]> baixarPlanilha(@PathVariable String id) {
        byte[] arquivo = planilhaJogadorService.gerarPlanilha(id);
        String nomeArquivo = "estatisticas-" + id + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(arquivo);
    }
}