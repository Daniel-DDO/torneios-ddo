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
import org.springframework.cache.annotation.Cacheable;
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

    @Autowired
    private EstiloGlobalCache estiloGlobalCache;

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
        return jogadorRepository.buscarRankingFinanceiro(pageable);
    }

    public ComparacaoJogadoresDTO compararJogadores(String idJogador1, String idJogador2) {
        JogadorComparacaoBaseDTO j1 = jogadorRepository.buscarBaseComparacaoPorId(idJogador1)
                .orElseThrow(() -> new RuntimeException("Jogador 1 não encontrado (ID: " + idJogador1 + ")"));
        JogadorComparacaoBaseDTO j2 = jogadorRepository.buscarBaseComparacaoPorId(idJogador2)
                .orElseThrow(() -> new RuntimeException("Jogador 2 não encontrado (ID: " + idJogador2 + ")"));

        List<PartidaHistoricoDTO> confrontos = partidaRepository.buscarConfrontosDiretos(idJogador1, idJogador2);

        int vitoriasJ1 = 0, vitoriasJ2 = 0, empates = 0;
        for (PartidaHistoricoDTO p : confrontos) {
            if (p.golsMandante() == null || p.golsVisitante() == null) continue;
            boolean mandanteEhJ1 = p.mandante().jogadorId().equals(idJogador1);
            int golsJ1 = mandanteEhJ1 ? p.golsMandante() : p.golsVisitante();
            int golsJ2 = mandanteEhJ1 ? p.golsVisitante() : p.golsMandante();
            if (golsJ1 > golsJ2) vitoriasJ1++;
            else if (golsJ2 > golsJ1) vitoriasJ2++;
            else empates++;
        }

        // 1 query em vez de 2
        AgregadoCasaForaParDTO cfPar = partidaRepository.buscarAgregadoCasaForaPar(idJogador1, idJogador2);
        EstatisticasCasaForaDTO cf1 = montarCasaFora(idJogador1, j1.nome(), j1.discord(), j1.imagem(), cfPar, true);
        EstatisticasCasaForaDTO cf2 = montarCasaFora(idJogador2, j2.nome(), j2.discord(), j2.imagem(), cfPar, false);

        // 1 query em vez de 2
        AgregadoEstiloParDTO estiloPar = jogadorClubeRepository.buscarAgregadoEstiloPar(idJogador1, idJogador2);
        double[] mediasGlobais = estiloGlobalCache.obterMediasGlobais();
        EstiloJogadorDTO estilo1 = montarEstilo(idJogador1, estiloPar.partidasJ1(), estiloPar.golsMarcadosJ1(), estiloPar.golsSofridosJ1(), estiloPar.mediaEstrelasJ1(), mediasGlobais);
        EstiloJogadorDTO estilo2 = montarEstilo(idJogador2, estiloPar.partidasJ2(), estiloPar.golsMarcadosJ2(), estiloPar.golsSofridosJ2(), estiloPar.mediaEstrelasJ2(), mediasGlobais);

        ComparacaoJogadoresDTO.FormaRecenteDTO forma1 = calcularFormaRecente(idJogador1);
        ComparacaoJogadoresDTO.FormaRecenteDTO forma2 = calcularFormaRecente(idJogador2);

        var dadosJ1 = mapearDadosComparacao(j1, cf1, estilo1, forma1);
        var dadosJ2 = mapearDadosComparacao(j2, cf2, estilo2, forma2);

        int jogos1 = nz(j1.partidasJogadas());
        int jogos2 = nz(j2.partidasJogadas());
        double aproveitamento1 = jogos1 > 0 ? ((nz(j1.vitorias()) * 3.0 + nz(j1.empates())) / (jogos1 * 3.0)) * 100.0 : 0.0;
        double aproveitamento2 = jogos2 > 0 ? ((nz(j2.vitorias()) * 3.0 + nz(j2.empates())) / (jogos2 * 3.0)) * 100.0 : 0.0;
        int saldoGols1 = nz(j1.golsMarcados()) - nz(j1.golsSofridos());
        int saldoGols2 = nz(j2.golsMarcados()) - nz(j2.golsSofridos());

        var analise = montarAnaliseComparativa(
                j1.nome(), estilo1.estiloProvavel(), cf1, forma1, aproveitamento1, saldoGols1, jogos1,
                j2.nome(), estilo2.estiloProvavel(), cf2, forma2, aproveitamento2, saldoGols2, jogos2
        );

        return new ComparacaoJogadoresDTO(
                dadosJ1, dadosJ2, confrontos,
                new ResumoConfrontoDiretoDTO(vitoriasJ1, vitoriasJ2, empates),
                analise
        );
    }

    private EstatisticasCasaForaDTO montarCasaFora(String jogadorId, String nome, String discord, String imagem,
                                                   AgregadoCasaForaParDTO p, boolean isJ1) {
        if (isJ1) {
            return new EstatisticasCasaForaDTO(jogadorId, nome, discord, imagem,
                    nz(p.vClubeCasaJ1()), nz(p.eClubeCasaJ1()), nz(p.dClubeCasaJ1()),
                    nz(p.vSelecaoCasaJ1()), nz(p.eSelecaoCasaJ1()), nz(p.dSelecaoCasaJ1()),
                    nz(p.vClubeForaJ1()), nz(p.eClubeForaJ1()), nz(p.dClubeForaJ1()),
                    nz(p.vSelecaoForaJ1()), nz(p.eSelecaoForaJ1()), nz(p.dSelecaoForaJ1()));
        }
        return new EstatisticasCasaForaDTO(jogadorId, nome, discord, imagem,
                nz(p.vClubeCasaJ2()), nz(p.eClubeCasaJ2()), nz(p.dClubeCasaJ2()),
                nz(p.vSelecaoCasaJ2()), nz(p.eSelecaoCasaJ2()), nz(p.dSelecaoCasaJ2()),
                nz(p.vClubeForaJ2()), nz(p.eClubeForaJ2()), nz(p.dClubeForaJ2()),
                nz(p.vSelecaoForaJ2()), nz(p.eSelecaoForaJ2()), nz(p.dSelecaoForaJ2()));
    }


    private ComparacaoJogadoresDTO.FormaRecenteDTO calcularFormaRecente(String jogadorId) {
        List<String> ultimos = partidaRepository.buscarUltimos5Resultados(jogadorId); // já existe

        int pontos = ultimos.stream().mapToInt(r -> switch (r) {
            case "V" -> 3;
            case "E" -> 1;
            default -> 0;
        }).sum();

        String tendencia;
        if (ultimos.size() < 3) {
            tendencia = "Amostra insuficiente";
        } else {
            int metadeRecente = 0, metadeAntiga = 0;
            int meio = ultimos.size() / 2;
            for (int i = 0; i < ultimos.size(); i++) {
                int valor = switch (ultimos.get(i)) { case "V" -> 3; case "E" -> 1; default -> 0; };
                if (i < meio) metadeAntiga += valor; else metadeRecente += valor;
            }
            tendencia = metadeRecente > metadeAntiga ? "Em ascensão"
                    : metadeRecente < metadeAntiga ? "Em queda"
                    : "Estável";
        }

        return new ComparacaoJogadoresDTO.FormaRecenteDTO(ultimos, pontos, tendencia);
    }

    private ComparacaoJogadoresDTO.AnaliseComparativaDTO montarAnaliseComparativa(
            String nome1, String estilo1, EstatisticasCasaForaDTO cf1, ComparacaoJogadoresDTO.FormaRecenteDTO forma1, double aproveitamento1, int saldoGols1, int jogos1,
            String nome2, String estilo2, EstatisticasCasaForaDTO cf2, ComparacaoJogadoresDTO.FormaRecenteDTO forma2, double aproveitamento2, int saldoGols2, int jogos2) {

        double vantagemMando1 = parsePercent(cf1.aproveitamentoCasa()) - parsePercent(cf1.aproveitamentoFora());
        double vantagemMando2 = parsePercent(cf2.aproveitamentoCasa()) - parsePercent(cf2.aproveitamentoFora());

        // Score combinado: aproveitamento geral (peso maior) + saldo de gols/jogo + forma recente
        double score1 = aproveitamento1
                + (jogos1 > 0 ? (saldoGols1 / (double) jogos1) * 10.0 : 0)
                + forma1.pontuacaoForma() * 1.5;

        double score2 = aproveitamento2
                + (jogos2 > 0 ? (saldoGols2 / (double) jogos2) * 10.0 : 0)
                + forma2.pontuacaoForma() * 1.5;

        double diferenca = Math.abs(score1 - score2);
        double margem = Math.min(100, diferenca); // simples, cap em 100

        String favorito = diferenca < 3
                ? "Equilibrado, sem favorito claro"
                : (score1 > score2 ? nome1 : nome2);

        String leitura = cruzarEstilos(estilo1, estilo2, nome1, nome2);

        List<String> pontosAtencao = new ArrayList<>();
        if (Math.abs(vantagemMando1) > 15) {
            pontosAtencao.add(nome1 + " tem forte dependência de mando de campo (" + String.format("%+.1f pp", vantagemMando1) + " jogando em casa)");
        }
        if (Math.abs(vantagemMando2) > 15) {
            pontosAtencao.add(nome2 + " tem forte dependência de mando de campo (" + String.format("%+.1f pp", vantagemMando2) + " jogando em casa)");
        }
        if (!forma1.tendencia().equals(forma2.tendencia())) {
            pontosAtencao.add("Momentos opostos: " + nome1 + " está \"" + forma1.tendencia().toLowerCase() +
                    "\" enquanto " + nome2 + " está \"" + forma2.tendencia().toLowerCase() + "\"");
        }

        return new ComparacaoJogadoresDTO.AnaliseComparativaDTO(
                round1(vantagemMando1), round1(vantagemMando2), favorito, round1(margem), leitura, pontosAtencao
        );
    }

    private String cruzarEstilos(String estilo1, String estilo2, String nome1, String nome2) {
        boolean j1ContraAtaque = estilo1.contains("contra-ataque");
        boolean j1Posse = estilo1.contains("posse de bola");
        boolean j1Retranca = estilo1.contains("retranca");
        boolean j2ContraAtaque = estilo2.contains("contra-ataque");
        boolean j2Posse = estilo2.contains("posse de bola");
        boolean j2Retranca = estilo2.contains("retranca");

        if (j1ContraAtaque && j2Posse) {
            return "Confronto favorável estilisticamente para " + nome1 + ": times de posse costumam deixar espaços que o contra-ataque de " + nome1 + " pode explorar (provável, com base no padrão histórico de jogo)";
        }
        if (j2ContraAtaque && j1Posse) {
            return "Confronto favorável estilisticamente para " + nome2 + ": mesma lógica invertida (provável)";
        }
        if (j1ContraAtaque && j2Retranca) {
            return "Confronto difícil para " + nome1 + ": contra-ataque tende a render pouco contra times fechados como o estilo de " + nome2 + " (provável)";
        }
        if (j2ContraAtaque && j1Retranca) {
            return "Confronto difícil para " + nome2 + " pelo mesmo motivo (provável)";
        }
        if (j1Retranca && j2Retranca) {
            return "Tendência de jogo truncado e com poucos gols entre os dois estilos (provável)";
        }
        if (j1Posse && j2Posse) {
            return "Tendência de jogo mais aberto e disputado no meio-campo, sem espaços fáceis pra nenhum dos dois (provável)";
        }
        return "Estilos sem cruzamento tático claro de vantagem — confronto mais equilibrado nesse critério (provável)";
    }

    private EstiloJogadorDTO montarEstilo(String jogadorId, Long partidasL, Long golsMarcadosL, Long golsSofridosL,
                                          Double mediaEstrelasObj, double[] mediasGlobais) {

        long partidasLong = nz(partidasL);
        if (partidasLong == 0) {
            throw new RegraNegocioException("Jogador não possui partidas suficientes para estimar um estilo de jogo.");
        }
        int partidas = (int) partidasLong;

        double mediaMarcados = nz(golsMarcadosL) / (double) partidas;
        double mediaSofridos = nz(golsSofridosL) / (double) partidas;
        double mediaEstrelas = nz(mediaEstrelasObj);

        double globalMarcados = mediasGlobais[0];
        double globalSofridos = mediasGlobais[1];
        double globalEstrelas = mediasGlobais[2];

        double difMarcados = globalMarcados == 0 ? 0 : (mediaMarcados - globalMarcados) / globalMarcados;
        double difSofridos = globalSofridos == 0 ? 0 : (mediaSofridos - globalSofridos) / globalSofridos;
        double difEstrelas = globalEstrelas == 0 ? 0 : (mediaEstrelas - globalEstrelas) / globalEstrelas;

        List<String> caracteristicas = new ArrayList<>();
        String estilo;

        boolean ataqueForte = difMarcados > 0.15;
        boolean ataqueFraco = difMarcados < -0.15;
        boolean defesaSolida = difSofridos < -0.15;
        boolean defesaFragil = difSofridos > 0.15;
        boolean timesFortes = difEstrelas > 0.10;

        if (ataqueForte && defesaFragil) {
            estilo = "Provavelmente ofensivo / trocação (joga aberto, marca e sofre muito)";
            caracteristicas.add("Ataque bem acima da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa abaixo da média, sofre mais que o normal (" + pct(difSofridos) + ")");
        } else if (ataqueForte && defesaSolida) {
            estilo = "Provavelmente posse de bola / controle de jogo (domina e sofre pouco)";
            caracteristicas.add("Ataque acima da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa sólida, sofre bem menos que a média (" + pct(difSofridos) + ")");
        } else if (ataqueFraco && defesaSolida) {
            estilo = "Provavelmente retranca / defensivo (prioriza não sofrer, ataca pouco)";
            caracteristicas.add("Ataque abaixo da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa sólida (" + pct(difSofridos) + ")");
        } else if (ataqueFraco && defesaFragil) {
            estilo = "Provavelmente irregular / sem padrão claro (ataca pouco e sofre bastante)";
            caracteristicas.add("Ataque abaixo da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa também abaixo da média (" + pct(difSofridos) + ")");
        } else if (defesaFragil) {
            estilo = "Provavelmente contra-ataque (ataque na média, mas sofre mais que o normal)";
            caracteristicas.add("Ataque próximo da média");
            caracteristicas.add("Sofre mais gols que a média (" + pct(difSofridos) + ")");
        } else {
            estilo = "Provavelmente equilibrado (ataque e defesa próximos da média geral)";
            caracteristicas.add("Ataque e defesa dentro da faixa normal");
        }

        if (timesFortes) {
            caracteristicas.add("Historicamente jogou em clubes acima da média de estrelas (" + pct(difEstrelas) + "), o que pode indicar mais tempo de posse típico desses elencos");
        } else if (difEstrelas < -0.10) {
            caracteristicas.add("Historicamente jogou em clubes abaixo da média de estrelas (" + pct(difEstrelas) + "), cenário mais propenso a contra-ataque por necessidade");
        }

        return new EstiloJogadorDTO(
                jogadorId, partidas,
                round2(mediaMarcados), round2(mediaSofridos), round2(mediaEstrelas),
                round2(globalMarcados), round2(globalSofridos), round2(globalEstrelas),
                estilo, caracteristicas
        );
    }

    private ComparacaoJogadoresDTO.DadosJogadorComparacao mapearDadosComparacao(
            JogadorComparacaoBaseDTO j,
            EstatisticasCasaForaDTO casaFora,
            EstiloJogadorDTO estilo,
            ComparacaoJogadoresDTO.FormaRecenteDTO formaRecente) {

        int jogos = nz(j.partidasJogadas());
        int vitorias = nz(j.vitorias());

        String aproveitamento = "0.0%";
        if (jogos > 0) {
            aproveitamento = String.format("%.1f%%", ((double) vitorias / jogos) * 100);
        }

        return new ComparacaoJogadoresDTO.DadosJogadorComparacao(
                j.id(), j.nome(), j.discord(), j.imagem(),
                nz(j.titulos()), nz(j.finais()), jogos, vitorias,
                nz(j.golsMarcados()), nz(j.golsSofridos()), aproveitamento,
                j.saldoVirtual() != null ? j.saldoVirtual() : BigDecimal.ZERO,
                j.pontosCoeficiente() != null ? j.pontosCoeficiente() : BigDecimal.ZERO,
                casaFora, estilo, formaRecente
        );
    }

    private double parsePercent(String pct) {
        return Double.parseDouble(pct.replace("%", "").replace(",", "."));
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }

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

    private long nz(Long valor) {
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

    public EstatisticasCasaForaDTO obterEstatisticasCasaFora(String jogadorId) {
        JogadorResumoDTO jogador = buscarResumoPorId(jogadorId);

        AgregadoCasaForaDTO a = partidaRepository.buscarAgregadoCasaFora(jogadorId);

        return new EstatisticasCasaForaDTO(
                jogadorId, jogador.nome(), jogador.discord(), jogador.imagem(),
                nz(a.vClubeCasa()), nz(a.eClubeCasa()), nz(a.dClubeCasa()),
                nz(a.vSelecaoCasa()), nz(a.eSelecaoCasa()), nz(a.dSelecaoCasa()),
                nz(a.vClubeFora()), nz(a.eClubeFora()), nz(a.dClubeFora()),
                nz(a.vSelecaoFora()), nz(a.eSelecaoFora()), nz(a.dSelecaoFora())
        );
    }

    @Transactional(readOnly = true)
    public MelhorTemporadaDTO obterMelhorTemporada(String jogadorId) {
        List<MelhorTemporadaDTO> resultado = jogadorClubeRepository
                .buscarMelhoresTemporadas(jogadorId, PageRequest.of(0, 1));

        if (resultado.isEmpty()) {
            throw new RegraNegocioException("Jogador não possui temporadas com partidas registradas.");
        }
        return resultado.get(0);
    }

    private double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    @Transactional(readOnly = true)
    public EstiloJogadorDTO obterEstiloProvavel(String jogadorId) {
        double[] globais = estiloGlobalCache.obterMediasGlobais();
        return obterEstiloProvavel(jogadorId, globais);
    }

    @Transactional(readOnly = true)
    public EstiloJogadorDTO obterEstiloProvavel(String jogadorId, double[] mediasGlobais) {
        AgregadoEstiloDTO agregado = jogadorClubeRepository.buscarAgregadoEstiloJogador(jogadorId);

        if (agregado == null || agregado.partidasJogadas() == null || agregado.partidasJogadas() == 0) {
            throw new RegraNegocioException("Jogador não possui partidas suficientes para estimar um estilo de jogo.");
        }

        int partidas = agregado.partidasJogadas().intValue();
        double mediaMarcados = agregado.golsMarcados() / (double) partidas;
        double mediaSofridos = agregado.golsSofridos() / (double) partidas;
        double mediaEstrelas = agregado.mediaEstrelas() == null ? 0.0 : agregado.mediaEstrelas();

        double globalMarcados = mediasGlobais[0];
        double globalSofridos = mediasGlobais[1];
        double globalEstrelas = mediasGlobais[2];

        // Posição relativa do jogador em relação à média global (%). > 0 = acima da média.
        double difMarcados = globalMarcados == 0 ? 0 : (mediaMarcados - globalMarcados) / globalMarcados;
        double difSofridos = globalSofridos == 0 ? 0 : (mediaSofridos - globalSofridos) / globalSofridos;
        double difEstrelas = globalEstrelas == 0 ? 0 : (mediaEstrelas - globalEstrelas) / globalEstrelas;

        List<String> caracteristicas = new ArrayList<>();
        String estilo;

        boolean ataqueForte = difMarcados > 0.15;
        boolean ataqueFraco = difMarcados < -0.15;
        boolean defesaSolida = difSofridos < -0.15;
        boolean defesaFragil = difSofridos > 0.15;
        boolean timesFortes = difEstrelas > 0.10;

        if (ataqueForte && defesaFragil) {
            estilo = "Provavelmente ofensivo / trocação (joga aberto, marca e sofre muito)";
            caracteristicas.add("Ataque bem acima da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa abaixo da média, sofre mais que o normal (" + pct(difSofridos) + ")");
        } else if (ataqueForte && defesaSolida) {
            estilo = "Provavelmente posse de bola / controle de jogo (domina e sofre pouco)";
            caracteristicas.add("Ataque acima da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa sólida, sofre bem menos que a média (" + pct(difSofridos) + ")");
        } else if (ataqueFraco && defesaSolida) {
            estilo = "Provavelmente retranca / defensivo (prioriza não sofrer, ataca pouco)";
            caracteristicas.add("Ataque abaixo da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa sólida (" + pct(difSofridos) + ")");
        } else if (ataqueFraco && defesaFragil) {
            estilo = "Provavelmente irregular / sem padrão claro (ataca pouco e sofre bastante)";
            caracteristicas.add("Ataque abaixo da média (" + pct(difMarcados) + ")");
            caracteristicas.add("Defesa também abaixo da média (" + pct(difSofridos) + ")");
        } else if (defesaFragil) {
            estilo = "Provavelmente contra-ataque (ataque na média, mas sofre mais que o normal)";
            caracteristicas.add("Ataque próximo da média");
            caracteristicas.add("Sofre mais gols que a média (" + pct(difSofridos) + ")");
        } else {
            estilo = "Provavelmente equilibrado (ataque e defesa próximos da média geral)";
            caracteristicas.add("Ataque e defesa dentro da faixa normal");
        }

        if (timesFortes) {
            caracteristicas.add("Historicamente jogou em clubes acima da média de estrelas (" + pct(difEstrelas) + "), o que pode indicar mais tempo de posse típico desses elencos");
        } else if (difEstrelas < -0.10) {
            caracteristicas.add("Historicamente jogou em clubes abaixo da média de estrelas (" + pct(difEstrelas) + "), cenário mais propenso a contra-ataque por necessidade");
        }

        return new EstiloJogadorDTO(
                jogadorId, partidas,
                round2(mediaMarcados), round2(mediaSofridos), round2(mediaEstrelas),
                round2(globalMarcados), round2(globalSofridos), round2(globalEstrelas),
                estilo, caracteristicas
        );
    }

    private String pct(double v) {
        return String.format("%+.1f%%", v * 100.0);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}