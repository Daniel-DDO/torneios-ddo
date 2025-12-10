package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorDTO;
import com.ddo.torneios.dto.LoginResponseDTO;
import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.exception.EmailJaCadastradoException;
import com.ddo.torneios.exception.JogadorExisteException;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.Avatar;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.request.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    public ResponseEntity<JogadorDTO> retornarJogador(String id) {
        return jogadorRepository.findById(id)
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
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado."));

        Avatar avatar = avatarService.buscarAvatarPorId(avatarId);
        jogador.setImagem(avatar.getUrl());

        return jogadorRepository.save(jogador);
    }

    @Transactional
    public void removerAvatar(String idJogador) {
        Jogador jogador = jogadorRepository.findById(idJogador)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        jogador.setImagem(null);
        jogadorRepository.save(jogador);
    }
}
