package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.EmailJaCadastradoException;
import com.ddo.torneios.exception.JogadorExisteException;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.exception.SaldoInsuficienteException;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JogadorService {

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ImgBBService imgBBService;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository;

    @Autowired
    private LanceRepository lanceRepository;

    @Autowired
    private TransferenciaRepository transferenciaRepository;

    public void cadastrarJogador(JogadorRequest request) {
        if (jogadorRepository.existsJogadorByDiscord(request.getDiscord())) {
            throw new JogadorExisteException(request.getDiscord());
        }

        Jogador jogador = new Jogador(request.getNome(), request.getDiscord());
        jogadorRepository.save(jogador);
    }

    public PaginacaoDTO<JogadorDTO> listarJogadores(
            String nomeFiltro,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Jogador> paginaEntidades;

        if (nomeFiltro != null && !nomeFiltro.isBlank()) {
            paginaEntidades = jogadorRepository.findByNomeContainingIgnoreCase(nomeFiltro, pageable);
        } else {
            paginaEntidades = jogadorRepository.findAll(pageable);
        }

        Page<JogadorDTO> paginaDTO = paginaEntidades.map(JogadorDTO::new);

        return new PaginacaoDTO<>(
                paginaDTO.getContent(),
                paginaDTO.getNumber(),
                paginaDTO.getTotalPages(),
                paginaDTO.getTotalElements(),
                paginaDTO.getSize(),
                paginaDTO.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<JogadorDTO> retornarJogador(String id) {
        return jogadorRepository.findComInsigniasById(id)
                .map(jogador -> ResponseEntity.ok(new JogadorDTO(jogador)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public String gerarCodigoReivindicacao(GerarCodigoRequest request) {
        Jogador admin = jogadorRepository.findById(request.getAdminId())
                .orElseThrow(() -> new RegraNegocioException("Admin não encontrado"));

        if (!isAdministrador(admin.getCargo())) {
            throw new RegraNegocioException("Sem permissão para gerar credenciais.");
        }

        Jogador alvo = jogadorRepository.findById(request.getJogadorId())
                .orElseThrow(() -> new RegraNegocioException("Jogador alvo não encontrado"));

        if (alvo.isContaReivindicada()) {
            throw new RegraNegocioException("Esta conta já foi reivindicada.");
        }

        String codigo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        alvo.setCodigoReivindicacao(codigo);
        alvo.setValidadeCodigoReivindicacao(LocalDateTime.now().plusHours(1));

        jogadorRepository.save(alvo);
        return codigo;
    }

    @Transactional
    public void reivindicarConta(ReivindicarContaRequest request) {
        Jogador jogador = jogadorRepository.findByDiscord(request.getDiscord())
                .orElseThrow(() -> new RegraNegocioException("Jogador não encontrado"));

        if (jogador.isContaReivindicada()) {
            throw new RegraNegocioException("Conta já reivindicada. Faça login.");
        }

        if (jogador.getCodigoReivindicacao() == null ||
                !jogador.getCodigoReivindicacao().equals(request.getCodigo())) {
            throw new RegraNegocioException("Código inválido ou incorreto.");
        }

        if (LocalDateTime.now().isAfter(jogador.getValidadeCodigoReivindicacao())) {
            throw new RegraNegocioException("O código expirou. Solicite um novo ao Admin.");
        }

        String novoEmail = request.getNovoEmail();

        if (!novoEmail.isBlank()) {
            if (jogadorRepository.existsJogadorByEmail(novoEmail)) {
                throw new EmailJaCadastradoException(novoEmail);
            }
            jogador.setEmail(novoEmail);
        }
        jogador.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        jogador.setContaReivindicada(true);

        jogador.setCodigoReivindicacao(null);
        jogador.setValidadeCodigoReivindicacao(null);

        jogadorRepository.save(jogador);
    }

    public LoginResponseDTO logarJogador(LoginRequest login) {
        String identificador = login.getLogin().trim();

        Optional<Jogador> jogadorOpt = jogadorRepository.findByDiscord(identificador);

        if (jogadorOpt.isEmpty()) {
            jogadorOpt = jogadorRepository.findByEmail(identificador);
        }

        Jogador jogador = jogadorOpt.orElseThrow(() ->
                new RegraNegocioException("Usuário não encontrado")
        );

        if (!jogador.isContaReivindicada()) {
            throw new RegraNegocioException("Conta não reivindicada. Solicite o código ao Admin.");
        }

        if (!passwordEncoder.matches(login.getSenha(), jogador.getSenha())) {
            throw new RegraNegocioException("Senha incorreta.");
        }

        String token = tokenService.gerarToken(jogador);
        return new LoginResponseDTO(token, new JogadorDTO(jogador));
    }

    public Integer consultarPinJogador(String idAdmin, String idJogadorAlvo) {
        Jogador admin = jogadorRepository.findById(idAdmin)
                .orElseThrow(() -> new RegraNegocioException("Admin não encontrado"));

        if (!isAdministrador(admin.getCargo())) {
            throw new RegraNegocioException("Você não tem permissão para ver PINs de jogadores.");
        }

        Jogador alvo = jogadorRepository.findById(idJogadorAlvo)
                .orElseThrow(() -> new RegraNegocioException("Jogador alvo não encontrado"));

        return alvo.getPin();
    }

    @Transactional
    public void recuperarSenhaComPin(RecuperarSenhaRequest request) {
        Jogador jogador = jogadorRepository.findByDiscord(request.getDiscord())
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        if (!jogador.isContaReivindicada()) {
            throw new RegraNegocioException("A conta não foi reivindicada ainda para tentar recuperar a senha.");
        }

        if (jogador.getPin() == null || !jogador.getPin().equals(request.getPin())) {
            throw new RegraNegocioException("PIN incorreto. Solicite o número correto ao Administrador.");
        }

        jogador.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        jogador.setPin(ThreadLocalRandom.current().nextInt(10000, 1000000));

        jogadorRepository.save(jogador);
    }

    private boolean isAdministrador(@NotNull Cargo cargo) {
        return cargo == Cargo.ADMINISTRADOR ||
                cargo == Cargo.DIRETOR ||
                cargo == Cargo.PROPRIETARIO;
    }

    @Transactional
    public String gerarPinsParaJogadoresLegados() {
        List<Jogador> jogadores = jogadorRepository.findAll();
        int count = 0;

        for (Jogador jogador : jogadores) {
            if (jogador.getPin() == null) {
                jogador.setPin(ThreadLocalRandom.current().nextInt(100000, 1000000));
                count++;
            }
        }

        jogadorRepository.saveAll(jogadores);
        return count + " jogadores tiveram seus PINs gerados com sucesso.";
    }

    @Transactional
    public void reivindicarContaDiretamente(ReivindicarDiretoRequest request) {
        Jogador jogador = jogadorRepository.findByDiscord(request.getDiscord())
                .orElseThrow(() -> new RegraNegocioException("Jogador não encontrado para o discord: " + request.getDiscord()));

        jogador.setEmail(request.getNovoEmail());
        jogador.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        jogador.setContaReivindicada(true);

        if (jogador.getPin() == null) {
            jogador.setPin(ThreadLocalRandom.current().nextInt(100000, 1000000));
        }

        jogador.setCodigoReivindicacao(null);
        jogador.setValidadeCodigoReivindicacao(null);

        if (request.getCargo() != null) {
            jogador.setCargo(request.getCargo());
        }

        jogadorRepository.save(jogador);
    }

    public List<Jogador> findByDiscordContainingIgnoreCase(String termo) {
        return jogadorRepository.findByDiscordContainingIgnoreCase(termo);
    }

    public List<JogadorResumo> buscarAutocomplete(String termo) {
        Pageable limit = PageRequest.of(0, 5);
        return jogadorRepository.findByDiscordContainingIgnoreCaseOrNomeContainingIgnoreCase(termo, termo, limit);
    }

    public Page<Jogador> listarTodosPaginado(Pageable pageable) {
        return jogadorRepository.findAll(pageable);
    }

    @Transactional
    public Jogador editarPerfilLogado(String idString, JogadorEditarRequest request) {

        Jogador jogador = jogadorRepository.findById(idString)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + idString));

        if (StringUtils.hasText(request.getNome())) {
            jogador.setNome(request.getNome());
        }

        if (StringUtils.hasText(request.getImagem())) {
            jogador.setImagem(request.getImagem());
        }

        if (StringUtils.hasText(request.getDescricao())) {
            jogador.setDescricao(request.getDescricao());
        }

        jogador.setModificacaoConta(LocalDateTime.now());
        return jogadorRepository.save(jogador);
    }

    @Transactional
    public Jogador atualizarFotoPerfil(String idJogador, MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new RuntimeException("Arquivo de imagem vazio.");
        }

        try {
            String urlImagem = imgBBService.uploadImagem(arquivo);

            Jogador jogador = jogadorRepository.findById(idJogador)
                    .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado"));

            jogador.setImagem(urlImagem);
            jogador.setModificacaoConta(LocalDateTime.now());

            return jogadorRepository.save(jogador);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar arquivo", e);
        }
    }

    public Jogador atualizarFotoPorAvatarId(String idJogador, String avatarId) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado com ID: " + idJogador));

        jogador.setImagem(avatarId);
        jogador.setModificacaoConta(LocalDateTime.now());
        return jogadorRepository.save(jogador);
    }

    @Transactional
    public void removerAvatar(String idJogador) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        jogador.setImagem(null);
        jogador.setModificacaoConta(LocalDateTime.now());
        jogadorRepository.save(jogador);
    }

    @Transactional
    public void alterarCredenciais(String idJogador, AlterarCredenciaisRequest request) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado"));

        if (!passwordEncoder.matches(request.getSenhaAtual(), jogador.getSenha())) {
            throw new RegraNegocioException("A senha atual informada está incorreta.");
        }

        if (StringUtils.hasText(request.getNovoEmail())) {
            if (!request.getNovoEmail().equalsIgnoreCase(jogador.getEmail())) {
                if (jogadorRepository.existsJogadorByEmail(request.getNovoEmail())) {
                    throw new EmailJaCadastradoException(request.getNovoEmail());
                }
                jogador.setEmail(request.getNovoEmail());
            }
        }

        if (StringUtils.hasText(request.getNovaSenha())) {
            if (passwordEncoder.matches(request.getNovaSenha(), jogador.getSenha())) {
                throw new RegraNegocioException("A nova senha não pode ser igual à senha atual.");
            }
            jogador.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        }

        jogador.setModificacaoConta(LocalDateTime.now());
        jogadorRepository.save(jogador);
    }

    public PaginacaoDTO<JogadorDTO> listarJogadoresDinamico(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());

        Page<Jogador> paginaEntidades = jogadorRepository.findAll(pageable);

        Page<JogadorDTO> paginaDTO = paginaEntidades.map(JogadorDTO::new);

        return new PaginacaoDTO<>(
                paginaDTO.getContent(),
                paginaDTO.getNumber(),
                paginaDTO.getTotalPages(),
                paginaDTO.getTotalElements(),
                paginaDTO.getSize(),
                paginaDTO.isLast()
        );
    }

    public Long contarContasReivindicadas() {
        return jogadorRepository.countByContaReivindicadaTrue();
    }

    @Transactional
    public JogadorDTO alterarCargo(String idJogador, Cargo novoCargo) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado"));
        jogador.setCargo(novoCargo);
        return new JogadorDTO(jogadorRepository.save(jogador));
    }

    public PaginacaoDTO<JogadorDTO> listarPorCargo(Cargo cargo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());

        Page<Jogador> paginaEntidades = jogadorRepository.findByCargo(cargo, pageable);

        Page<JogadorDTO> paginaDTO = paginaEntidades.map(JogadorDTO::new);

        return new PaginacaoDTO<>(
                paginaDTO.getContent(),
                paginaDTO.getNumber(),
                paginaDTO.getTotalPages(),
                paginaDTO.getTotalElements(),
                paginaDTO.getSize(),
                paginaDTO.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<JogadorDTO> buscarJogadorAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }

        Pageable limit = PageRequest.of(0, 5);

        return jogadorRepository.buscarAutocomplete(termo.trim(), limit)
                .stream()
                .map(JogadorDTO::new)
                .toList();
    }

    public List<JogadorResumoDTO> buscarJogadoresParaSelect(String termo) {
        return jogadorRepository.findByNomeContainingIgnoreCaseOrDiscordContainingIgnoreCase(termo, termo)
                .stream()
                .map(j -> new JogadorResumoDTO(j.getId(), j.getNome(), j.getDiscord(), j.getPontosCoeficiente(), j.getImagem()))
                .collect(Collectors.toList());
    }

    public List<JogadorResumoDTO> retornarTodosJogadoresMelhorCoeficiente() {
        return jogadorRepository.buscarRankingCompleto();
    }

    public List<JogadorResumoDTO> retornarTop10JogadoresMelhorCoeficiente() {
        return jogadorRepository.buscarTop10Ranking();
    }

    public JogadorHistoriaDTO obterResumoHistoria(String jogadorId) {
        Jogador j = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado"));

        int jogos = j.getPartidasJogadas() != null ? j.getPartidasJogadas() : 0;
        int vitorias = j.getVitorias() != null ? j.getVitorias() : 0;
        int empates = j.getEmpates() != null ? j.getEmpates() : 0;
        int derrotas = j.getDerrotas() != null ? j.getDerrotas() : 0;
        int gm = j.getGolsMarcados() != null ? j.getGolsMarcados() : 0;
        int gs = j.getGolsSofridos() != null ? j.getGolsSofridos() : 0;

        double aproveitamento = 0.0;
        if (jogos > 0) {
            double pontosConquistados = (vitorias * 3.0) + empates;
            double pontosPossiveis = jogos * 3.0;
            aproveitamento = (pontosConquistados / pontosPossiveis) * 100.0;
        }

        double mediaGols = (jogos > 0) ? (double) gm / jogos : 0.0;

        return new JogadorHistoriaDTO(
                j.getId(),
                j.getNome(),
                j.getImagem(),
                j.getCargo().name(),
                jogos,
                vitorias,
                empates,
                derrotas,
                String.format("%.1f%%", aproveitamento),
                gm,
                gs,
                (gm - gs),
                Math.round(mediaGols * 100.0) / 100.0,
                j.getTitulos() != null ? j.getTitulos() : 0,
                j.getFinais() != null ? j.getFinais() : 0,
                j.getPontosCoeficiente()
        );
    }

    @Transactional
    public void alterarStatusJogador(String idJogador, StatusJogador novoStatus) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + idJogador));

        if (novoStatus == StatusJogador.ATIVO) {
            jogador.setSuspensoAte(null);
        }

        jogador.setStatusJogador(novoStatus);
        jogadorRepository.save(jogador);
    }

    public List<RivalidadeDTO> buscarTop3Patos(String jogadorId) {
        return mapearRivalidades(partidaRepository.findTop3Patos(jogadorId));
    }

    public List<RivalidadeDTO> buscarTop3Carrascos(String jogadorId) {
        return mapearRivalidades(partidaRepository.findTop3Carrascos(jogadorId));
    }

    private List<RivalidadeDTO> mapearRivalidades(List<PatoProjection> projecoes) {
        return projecoes.stream().map(p -> {
            RivalidadeDTO dto = new RivalidadeDTO();
            dto.setAdversarioId(p.getAdversarioId());
            dto.setAdversarioNome(p.getAdversarioNome());
            dto.setAdversarioDiscord(p.getAdversarioDiscord());
            dto.setAdversarioImagem(p.getAdversarioImagem());

            dto.setPartidasJogadas(p.getTotalJogos());
            dto.setMinhasVitorias(p.getMinhasVitorias());
            dto.setMeusEmpates(p.getMeusEmpates());
            dto.setMinhasDerrotas(p.getTotalJogos() - p.getMinhasVitorias() - p.getMeusEmpates());

            dto.setGolsFeitos(p.getMeusGols());
            dto.setGolsSofridos(p.getGolsSofridos());
            dto.setSaldoGols(p.getMeusGols() - p.getGolsSofridos());

            dto.setAproveitamento(calcularAproveitamento(p.getMinhasVitorias(), p.getMeusEmpates(), p.getTotalJogos()));

            return dto;
        }).collect(Collectors.toList());
    }

    private String calcularAproveitamento(int vitorias, int empates, int totalJogos) {
        if (totalJogos == 0) return "0.0%";
        double pontos = (vitorias * 3.0) + empates;
        double possiveis = totalJogos * 3.0;
        return String.format("%.1f%%", (pontos / possiveis) * 100.0);
    }

    @Transactional
    public BigDecimal atualizarSaldo(String jogadorId, MovimentacaoSaldoDTO dto, String idAdminResponsavel) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        BigDecimal saldoAtual = jogador.getSaldoVirtual();
        BigDecimal novoSaldo;
        TipoTransacao tipoTransacao;

        if (dto.operacao() == MovimentacaoSaldoDTO.TipoOperacao.ADICIONAR) {
            novoSaldo = saldoAtual.add(dto.valor());
            tipoTransacao = TipoTransacao.CREDITO;
        } else {
            boolean vaiFicarNegativo = saldoAtual.compareTo(dto.valor()) < 0;

            if (vaiFicarNegativo && !dto.confirmarSaldoNegativo()) {
                throw new SaldoInsuficienteException("O saldo ficará negativo (" +
                        saldoAtual.subtract(dto.valor()) + "). Confirma a operação?");
            }

            novoSaldo = saldoAtual.subtract(dto.valor());
            tipoTransacao = TipoTransacao.DEBITO;
        }

        Transacao transacao = new Transacao(
                jogador,
                tipoTransacao,
                dto.valor(),
                saldoAtual,
                novoSaldo,
                dto.motivo(),
                idAdminResponsavel
        );

        jogador.setSaldoVirtual(novoSaldo);

        transacaoRepository.save(transacao);
        jogadorRepository.save(jogador);

        enviarNotificacaoSaldo(jogador, dto);

        return novoSaldo;
    }

    public List<String> obterMomentoAtual(String jogadorId) {
        return partidaRepository.buscarUltimos5Resultados(jogadorId);
    }

    public Page<TransacaoResponseDTO> listarTransacoesDoJogador(String jogadorId, Pageable pageable) {
        if (!jogadorRepository.existsById(jogadorId)) {
            throw new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId);
        }

        return transacaoRepository.findByJogadorIdOrderByDataHoraDesc(jogadorId, pageable)
                .map(t -> new TransacaoResponseDTO(
                        t.getId(),
                        t.getTipo(),
                        t.getValor(),
                        t.getSaldoAnterior(),
                        t.getSaldoPosterior(),
                        t.getMotivo(),
                        t.getResponsavel(),
                        t.getDataHora()
                ));
    }

    @Value("${app.frontend.url}")
    private String linkFront;

    private void enviarNotificacaoSaldo(Jogador jogador, MovimentacaoSaldoDTO dto) {
        try {
            String valorFormatado = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"))
                    .format(dto.valor());

            String titulo;
            String mensagem;
            TipoNotificacao tipo;

            if (dto.operacao() == MovimentacaoSaldoDTO.TipoOperacao.ADICIONAR) {
                titulo = "Saldo Recebido!";
                mensagem = String.format("Você recebeu %s. Motivo: %s", valorFormatado, dto.motivo());
                tipo = TipoNotificacao.INFORMACAO;
            } else {
                titulo = "Pagamento Realizado";
                mensagem = String.format("Foi debitado %s da sua conta. Motivo: %s", valorFormatado, dto.motivo());
                tipo = TipoNotificacao.INFORMACAO;
            }

            String link = linkFront + "/minha-conta/financeiro";

            notificacaoService.enviarParaJogador(
                    jogador,
                    titulo,
                    mensagem,
                    link,
                    tipo
            );
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de saldo para o jogador {}", jogador.getId(), e);
        }
    }

    public Page<JogadorRankingDTO> listarRankingFinanceiro(Pageable pageable) {
        return jogadorRepository.findAllByOrderBySaldoVirtualDesc(pageable)
                .map(jogador -> new JogadorRankingDTO(
                        jogador.getId(),
                        jogador.getNome(),
                        jogador.getDiscord(),
                        jogador.getImagem(),
                        jogador.getCargo().name(),
                        jogador.getSaldoVirtual() != null ? jogador.getSaldoVirtual() : BigDecimal.ZERO
                ));
    }

    public ComparacaoJogadoresDTO compararJogadores(String idJogador1, String idJogador2) {
        Jogador j1 = jogadorRepository.findById(idJogador1)
                .orElseThrow(() -> new RuntimeException("Jogador 1 não encontrado (ID: " + idJogador1 + ")"));

        Jogador j2 = jogadorRepository.findById(idJogador2)
                .orElseThrow(() -> new RuntimeException("Jogador 2 não encontrado (ID: " + idJogador2 + ")"));

        List<PartidaHistoricoDTO> confrontos = partidaRepository.buscarConfrontosDiretos(idJogador1, idJogador2);

        int vitoriasJ1 = 0;
        int vitoriasJ2 = 0;
        int empates = 0;

        for (PartidaHistoricoDTO p : confrontos) {
            if (p.golsMandante() == null || p.golsVisitante() == null) continue;

            boolean mandanteEhJ1 = p.mandante().jogadorId().equals(idJogador1);

            int golsJ1 = mandanteEhJ1 ? p.golsMandante() : p.golsVisitante();
            int golsJ2 = mandanteEhJ1 ? p.golsVisitante() : p.golsMandante();

            if (golsJ1 > golsJ2) {
                vitoriasJ1++;
            } else if (golsJ2 > golsJ1) {
                vitoriasJ2++;
            } else {
                empates++;
            }
        }

        return new ComparacaoJogadoresDTO(
                mapearDadosComparacao(j1),
                mapearDadosComparacao(j2),
                confrontos,
                new ResumoConfrontoDiretoDTO(vitoriasJ1, vitoriasJ2, empates)
        );
    }

    private ComparacaoJogadoresDTO.DadosJogadorComparacao mapearDadosComparacao(Jogador j) {
        int jogos = j.getPartidasJogadas() != null ? j.getPartidasJogadas() : 0;
        int vitorias = j.getVitorias() != null ? j.getVitorias() : 0;

        String aproveitamento = "0.0%";
        if (jogos > 0) {
            double pct = ((double) vitorias / jogos) * 100;
            aproveitamento = String.format("%.1f%%", pct);
        }

        return new ComparacaoJogadoresDTO.DadosJogadorComparacao(
                j.getId(),
                j.getNome(),
                j.getDiscord(),
                j.getImagem(),
                j.getTitulos() != null ? j.getTitulos() : 0,
                j.getFinais() != null ? j.getFinais() : 0,
                jogos,
                vitorias,
                j.getGolsMarcados() != null ? j.getGolsMarcados() : 0,
                j.getGolsSofridos() != null ? j.getGolsSofridos() : 0,
                aproveitamento,
                j.getSaldoVirtual() != null ? j.getSaldoVirtual() : BigDecimal.ZERO,
                j.getPontosCoeficiente() != null ? j.getPontosCoeficiente() : BigDecimal.ZERO
        );
    }

    public JogadorResumoDTO buscarResumoPorId(String id) {
        return jogadorRepository.findResumoById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + id));
    }

    @Transactional
    public JogadorDTO trocarDiscord(String jogadorId, String novoDiscord) {
        if (!StringUtils.hasText(novoDiscord)) {
            throw new RegraNegocioException("O novo discord não pode ser vazio.");
        }

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        if (!novoDiscord.equalsIgnoreCase(jogador.getDiscord()) && jogadorRepository.existsJogadorByDiscord(novoDiscord)) {
            throw new RegraNegocioException("Este discord já está em uso por outro jogador.");
        }

        jogador.setDiscord(novoDiscord);
        jogador.setModificacaoConta(LocalDateTime.now());

        return new JogadorDTO(jogadorRepository.save(jogador));
    }

    @Transactional
    public JogadorDTO atualizarEmailAdmin(String jogadorId, String novoEmail) {
        if (!StringUtils.hasText(novoEmail)) {
            throw new RegraNegocioException("O novo email não pode ser vazio.");
        }

        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        if (!novoEmail.equalsIgnoreCase(jogador.getEmail()) && jogadorRepository.existsJogadorByEmail(novoEmail)) {
            throw new EmailJaCadastradoException(novoEmail);
        }

        jogador.setEmail(novoEmail);
        jogador.setModificacaoConta(LocalDateTime.now());

        return new JogadorDTO(jogadorRepository.save(jogador));
    }

    @Transactional
    public BigDecimal zerarSaldoJogador(String jogadorId, String motivo, String idAdminResponsavel) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        BigDecimal saldoAnterior = jogador.getSaldoVirtual() != null ? jogador.getSaldoVirtual() : BigDecimal.ZERO;

        if (saldoAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return saldoAnterior;
        }

        Transacao transacao = new Transacao(
                jogador,
                TipoTransacao.DEBITO,
                saldoAnterior,
                saldoAnterior,
                BigDecimal.ZERO,
                StringUtils.hasText(motivo) ? motivo : "Zeragem administrativa de saldo",
                idAdminResponsavel
        );

        jogador.setSaldoVirtual(BigDecimal.ZERO);
        jogador.setModificacaoConta(LocalDateTime.now());

        transacaoRepository.save(transacao);
        jogadorRepository.save(jogador);

        return BigDecimal.ZERO;
    }

    @Transactional
    public int zerarSaldoDeTodosOsJogadores(String motivo, String idAdminResponsavel) {
        List<SaldoProjecaoDTO> saldos = jogadorRepository.buscarSaldosDeJogadoresAtivos();

        String motivoFinal = StringUtils.hasText(motivo) ? motivo : "Zeragem administrativa de saldo (em massa)";

        List<Transacao> transacoes = saldos.stream()
                .filter(s -> s.saldoVirtual() != null && s.saldoVirtual().compareTo(BigDecimal.ZERO) != 0)
                .map(s -> new Transacao(
                        jogadorRepository.getReferenceById(s.id()),
                        TipoTransacao.DEBITO,
                        s.saldoVirtual(),
                        s.saldoVirtual(),
                        BigDecimal.ZERO,
                        motivoFinal,
                        idAdminResponsavel
                ))
                .toList();

        jogadorRepository.zerarSaldoDeTodosOsJogadores(BigDecimal.ZERO);
        transacaoRepository.saveAll(transacoes);

        return transacoes.size();
    }

    @Transactional
    public int distribuirSaldoParaTodos(BigDecimal valor, String motivo, String idAdminResponsavel) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor a distribuir deve ser maior que zero.");
        }

        List<SaldoProjecaoDTO> saldos = jogadorRepository.buscarSaldosDeJogadoresAtivos();

        String motivoFinal = StringUtils.hasText(motivo) ? motivo : "Distribuição administrativa de saldo";

        List<Transacao> transacoes = saldos.stream()
                .map(s -> {
                    BigDecimal saldoAnterior = s.saldoVirtual() != null ? s.saldoVirtual() : BigDecimal.ZERO;
                    BigDecimal saldoPosterior = saldoAnterior.add(valor);
                    return new Transacao(
                            jogadorRepository.getReferenceById(s.id()),
                            TipoTransacao.CREDITO,
                            valor,
                            saldoAnterior,
                            saldoPosterior,
                            motivoFinal,
                            idAdminResponsavel
                    );
                })
                .toList();

        jogadorRepository.distribuirSaldoParaTodosOsJogadores(valor);
        transacaoRepository.saveAll(transacoes);

        return transacoes.size();
    }

    @Transactional
    public void resetarSenhaAdmin(String jogadorId, String novaSenha) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        jogador.setSenha(passwordEncoder.encode(novaSenha));
        jogador.setModificacaoConta(LocalDateTime.now());
        jogadorRepository.save(jogador);
    }

    @Transactional
    public Integer resetarPinAdmin(String jogadorId) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        Integer novoPin = ThreadLocalRandom.current().nextInt(100000, 1000000);
        jogador.setPin(novoPin);
        jogadorRepository.save(jogador);

        return novoPin;
    }

    @Transactional
    public void deletarJogador(String jogadorId) {
        Jogador jogador = jogadorRepository.findById(jogadorId)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + jogadorId));

        if (jogador.isContaReivindicada()) {
            throw new RegraNegocioException("Não é possível deletar uma conta já reivindicada. Suspenda o jogador em vez disso.");
        }

        jogadorRepository.delete(jogador);
    }

    @Transactional
    public Jogador mesclarContas(String idPrincipal, List<String> idsAntigosRequest) {

        List<String> idsAntigos = idsAntigosRequest.stream().distinct().toList();

        if (idsAntigos.contains(idPrincipal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A conta principal não pode estar na lista de contas antigas.");
        }

        Jogador principal = jogadorRepository.findById(idPrincipal)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta principal não encontrada."));

        List<Jogador> antigos = jogadorRepository.findAllById(idsAntigos);
        if (antigos.size() != idsAntigos.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais contas antigas não foram encontradas.");
        }

        for (Jogador antigo : antigos) {
            principal.setFinais(nz(principal.getFinais()) + nz(antigo.getFinais()));
            principal.setTitulos(nz(principal.getTitulos()) + nz(antigo.getTitulos()));
            principal.setGolsMarcados(nz(principal.getGolsMarcados()) + nz(antigo.getGolsMarcados()));
            principal.setGolsSofridos(nz(principal.getGolsSofridos()) + nz(antigo.getGolsSofridos()));
            principal.setPartidasJogadas(nz(principal.getPartidasJogadas()) + nz(antigo.getPartidasJogadas()));
            principal.setVitorias(nz(principal.getVitorias()) + nz(antigo.getVitorias()));
            principal.setEmpates(nz(principal.getEmpates()) + nz(antigo.getEmpates()));
            principal.setDerrotas(nz(principal.getDerrotas()) + nz(antigo.getDerrotas()));
            principal.setCartoesAmarelos(nzLong(principal.getCartoesAmarelos()) + nzLong(antigo.getCartoesAmarelos()));
            principal.setCartoesVermelhos(nzLong(principal.getCartoesVermelhos()) + nzLong(antigo.getCartoesVermelhos()));
            principal.setSaldoVirtual(nzBig(principal.getSaldoVirtual()).add(nzBig(antigo.getSaldoVirtual())));
            principal.setPontosCoeficiente(nzBig(principal.getPontosCoeficiente()).add(nzBig(antigo.getPontosCoeficiente())));

            principal.getInsignias().addAll(antigo.getInsignias());

            for (Conquista conquista : new ArrayList<>(antigo.getConquistas())) {
                conquista.setJogador(principal);
                principal.getConquistas().add(conquista);
            }
            antigo.getConquistas().clear();
        }
        principal.setModificacaoConta(LocalDateTime.now());

        mesclarJogadorClube(idPrincipal, idsAntigos, principal);

        transacaoRepository.reatribuirJogador(idsAntigos, principal);
        transferenciaRepository.reatribuirJogador(idsAntigos, principal);

        mesclarLances(idPrincipal, idsAntigos, principal);

        jogadorRepository.deleteAll(antigos);

        return jogadorRepository.save(principal);
    }

    private void mesclarJogadorClube(String idPrincipal, List<String> idsAntigos, Jogador principal) {
        Map<String, String> temporadaParaJcPrincipal = new HashMap<>();
        for (Object[] linha : jogadorClubeRepository.buscarIdETemporadaPorJogador(idPrincipal)) {
            temporadaParaJcPrincipal.put((String) linha[1], (String) linha[0]);
        }

        List<String> reatribuicaoSimples = new ArrayList<>();
        List<String[]> conflitos = new ArrayList<>();

        for (Object[] linha : jogadorClubeRepository.buscarIdETemporadaPorJogadores(idsAntigos)) {
            String jcId = (String) linha[0];
            String temporadaId = (String) linha[1];
            String sobreviventeId = temporadaParaJcPrincipal.get(temporadaId);

            if (sobreviventeId == null) {
                reatribuicaoSimples.add(jcId);
                temporadaParaJcPrincipal.put(temporadaId, jcId);
            } else {
                conflitos.add(new String[]{jcId, sobreviventeId});
            }
        }

        if (!reatribuicaoSimples.isEmpty()) {
            jogadorClubeRepository.reatribuirJogador(reatribuicaoSimples, principal);
        }

        for (String[] par : conflitos) {
            mesclarJogadorClubeComConflito(par[0], par[1]);
        }
    }

    private void mesclarJogadorClubeComConflito(String antigoId, String sobreviventeId) {
        JogadorClube antigo = jogadorClubeRepository.findById(antigoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JogadorClube antigo não encontrado."));
        JogadorClube sobrevivente = jogadorClubeRepository.findById(sobreviventeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JogadorClube sobrevivente não encontrado."));

        sobrevivente.setTotalGolsMarcados(nz(sobrevivente.getTotalGolsMarcados()) + nz(antigo.getTotalGolsMarcados()));
        sobrevivente.setTotalGolsSofridos(nz(sobrevivente.getTotalGolsSofridos()) + nz(antigo.getTotalGolsSofridos()));
        sobrevivente.setTotalCartoesAmarelos(nz(sobrevivente.getTotalCartoesAmarelos()) + nz(antigo.getTotalCartoesAmarelos()));
        sobrevivente.setTotalCartoesVermelhos(nz(sobrevivente.getTotalCartoesVermelhos()) + nz(antigo.getTotalCartoesVermelhos()));
        sobrevivente.setPartidasJogadas(nz(sobrevivente.getPartidasJogadas()) + nz(antigo.getPartidasJogadas()));
        sobrevivente.setVitorias(nz(sobrevivente.getVitorias()) + nz(antigo.getVitorias()));
        sobrevivente.setEmpates(nz(sobrevivente.getEmpates()) + nz(antigo.getEmpates()));
        sobrevivente.setDerrotas(nz(sobrevivente.getDerrotas()) + nz(antigo.getDerrotas()));
        sobrevivente.setBalancoFinanceiro(nzBig(sobrevivente.getBalancoFinanceiro()).add(nzBig(antigo.getBalancoFinanceiro())));
        sobrevivente.setPontosCoeficiente(nzBig(sobrevivente.getPontosCoeficiente()).add(nzBig(antigo.getPontosCoeficiente())));

        int jogos = nz(sobrevivente.getPartidasJogadas());
        sobrevivente.setAproveitamento(jogos > 0
                ? (nz(sobrevivente.getVitorias()) * 3 + nz(sobrevivente.getEmpates())) * 100.0 / (jogos * 3)
                : 0.0);

        partidaRepository.reatribuirMandante(antigoId, sobreviventeId);
        partidaRepository.reatribuirVisitante(antigoId, sobreviventeId);

        List<ParticipacaoFase> participacoesAntigas = participacaoFaseRepository.findByJogadorClube_Id(antigoId);
        for (ParticipacaoFase pAntiga : participacoesAntigas) {
            Optional<ParticipacaoFase> pSobreviventeOpt = participacaoFaseRepository
                    .findByFase_IdAndJogadorClube_Id(pAntiga.getFase().getId(), sobreviventeId);

            if (pSobreviventeOpt.isEmpty()) {
                pAntiga.setJogadorClube(sobrevivente);
                pAntiga.getHistoricoJogadorClubeIds().add(antigoId);
            } else {
                ParticipacaoFase pSobrevivente = pSobreviventeOpt.get();
                pSobrevivente.setPontos(nz(pSobrevivente.getPontos()) + nz(pAntiga.getPontos()));
                pSobrevivente.setPartidasJogadas(nz(pSobrevivente.getPartidasJogadas()) + nz(pAntiga.getPartidasJogadas()));
                pSobrevivente.setVitorias(nz(pSobrevivente.getVitorias()) + nz(pAntiga.getVitorias()));
                pSobrevivente.setEmpates(nz(pSobrevivente.getEmpates()) + nz(pAntiga.getEmpates()));
                pSobrevivente.setDerrotas(nz(pSobrevivente.getDerrotas()) + nz(pAntiga.getDerrotas()));
                pSobrevivente.setGolsPro(nz(pSobrevivente.getGolsPro()) + nz(pAntiga.getGolsPro()));
                pSobrevivente.setGolsContra(nz(pSobrevivente.getGolsContra()) + nz(pAntiga.getGolsContra()));
                pSobrevivente.setSaldoGols(pSobrevivente.getGolsPro() - pSobrevivente.getGolsContra());
                pSobrevivente.getHistoricoJogadorClubeIds().add(antigoId);

                participacaoFaseRepository.delete(pAntiga);
            }
        }

        jogadorClubeRepository.delete(antigo);
    }

    private void mesclarLances(String idPrincipal, List<String> idsAntigos, Jogador principal) {
        record ChaveLance(String leilaoId, Integer prioridade) {}

        Map<ChaveLance, String[]> lancesPorChave = new HashMap<>();
        for (Object[] linha : lanceRepository.buscarChavesPorJogador(idPrincipal)) {
            ChaveLance chave = new ChaveLance((String) linha[1], (Integer) linha[2]);
            lancesPorChave.put(chave, new String[]{(String) linha[0], linha[3].toString()});
        }

        List<String> reatribuicaoSimples = new ArrayList<>();
        List<String> paraDeletar = new ArrayList<>();

        for (Object[] linha : lanceRepository.buscarChavesPorJogadores(idsAntigos)) {
            String lanceAntigoId = (String) linha[0];
            ChaveLance chave = new ChaveLance((String) linha[1], (Integer) linha[2]);
            BigDecimal valorAntigo = (BigDecimal) linha[3];

            String[] existente = lancesPorChave.get(chave);

            if (existente == null) {
                lancesPorChave.put(chave, new String[]{lanceAntigoId, valorAntigo.toString()});
                reatribuicaoSimples.add(lanceAntigoId);
            } else {
                BigDecimal valorExistente = new BigDecimal(existente[1]);
                if (valorAntigo.compareTo(valorExistente) > 0) {
                    paraDeletar.add(existente[0]);
                    reatribuicaoSimples.remove(existente[0]);
                    lancesPorChave.put(chave, new String[]{lanceAntigoId, valorAntigo.toString()});
                    reatribuicaoSimples.add(lanceAntigoId);
                } else {
                    paraDeletar.add(lanceAntigoId);
                }
            }
        }

        if (!reatribuicaoSimples.isEmpty()) {
            lanceRepository.reatribuirJogador(reatribuicaoSimples, principal);
        }
        if (!paraDeletar.isEmpty()) {
            lanceRepository.deletarPorIds(paraDeletar);
        }
    }

    private int nz(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private long nzLong(Long valor) {
        return valor == null ? 0L : valor;
    }

    private BigDecimal nzBig(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    @Transactional
    public JogadorDTO editarJogadorAdmin(String id, JogadorEditarRequest request) {
        Jogador jogador = jogadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + id));

        if (StringUtils.hasText(request.getNome())) {
            jogador.setNome(request.getNome());
        }

        if (StringUtils.hasText(request.getImagem())) {
            jogador.setImagem(request.getImagem());
        }

        if (StringUtils.hasText(request.getDescricao())) {
            jogador.setDescricao(request.getDescricao());
        }

        jogador.setModificacaoConta(LocalDateTime.now());

        return new JogadorDTO(jogadorRepository.save(jogador));
    }
}