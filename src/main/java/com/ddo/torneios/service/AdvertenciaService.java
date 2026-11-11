package com.ddo.torneios.service;

import com.ddo.torneios.dto.AdvertenciaPublicaDTO;
import com.ddo.torneios.dto.AplicarAdvertenciaRequest;
import com.ddo.torneios.dto.ContagemNivelDTO;
import com.ddo.torneios.dto.ContagemNivelProjecao;
import com.ddo.torneios.dto.DecidirRecursoRequest;
import com.ddo.torneios.dto.FichaAdvertenciasDTO;
import com.ddo.torneios.event.SuspensaoPorAdvertenciaCanceladaEvent;
import com.ddo.torneios.event.SuspensaoPorAdvertenciaGeradaEvent;
import com.ddo.torneios.model.Advertencia;
import com.ddo.torneios.model.Cargo;
import com.ddo.torneios.model.NivelAdvertencia;
import com.ddo.torneios.model.OrigemSuspensaoAdvertencia;
import com.ddo.torneios.model.ResultadoRecurso;
import com.ddo.torneios.model.StatusAdvertencia;
import com.ddo.torneios.model.SuspensaoPorAdvertencia;
import com.ddo.torneios.model.TipoSuspensaoAdvertencia;
import com.ddo.torneios.repository.AdvertenciaRepository;
import com.ddo.torneios.repository.JogadorAdvertenciaRepository;
import com.ddo.torneios.repository.SuspensaoPorAdvertenciaRepository;
import com.ddo.torneios.util.RelogioBrasilia;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AdvertenciaService {

    private static final int MULTIPLICADOR_PADRAO = 1;
    private static final int MULTIPLICADOR_MAXIMO_SUPERINTENDENCIA = 10;
    private static final int PARTIDAS_PADRAO = 1;
    private static final long HORAS_PRAZO_RECURSO = 24L;

    private final AdvertenciaRepository advertenciaRepository;
    private final SuspensaoPorAdvertenciaRepository suspensaoRepository;
    private final JogadorAdvertenciaRepository jogadorRepository;
    private final ApplicationEventPublisher publicador;

    public AdvertenciaService(AdvertenciaRepository advertenciaRepository,
                              SuspensaoPorAdvertenciaRepository suspensaoRepository,
                              JogadorAdvertenciaRepository jogadorRepository,
                              ApplicationEventPublisher publicador) {
        this.advertenciaRepository = advertenciaRepository;
        this.suspensaoRepository = suspensaoRepository;
        this.jogadorRepository = jogadorRepository;
        this.publicador = publicador;
    }

    @Transactional
    public AdvertenciaPublicaDTO aplicar(String jogadorId, String aplicadorId, AplicarAdvertenciaRequest requisicao) {
        travarJogador(jogadorId);
        Cargo cargoAplicador = jogadorRepository.buscarCargo(aplicadorId)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Aplicador não encontrado"));

        NivelAdvertencia nivel = requisicao.nivel();
        validarPermissao(cargoAplicador, nivel);

        int multiplicador = requisicao.multiplicador() == null ? MULTIPLICADOR_PADRAO : requisicao.multiplicador();
        validarMultiplicador(multiplicador, requisicao.fundamentacao(), cargoAplicador);

        OffsetDateTime agora = RelogioBrasilia.agora();
        boolean emRegime = advertenciaRepository.existsByJogadorIdAndNivelAndStatusAndValidaAteAfter(
                jogadorId, NivelAdvertencia.GRAVISSIMA, StatusAdvertencia.ATIVA, agora);
        boolean suspensaoImediata = emRegime && nivel != NivelAdvertencia.LEVE;

        if (suspensaoImediata && nivel == NivelAdvertencia.GRAVISSIMA && requisicao.diasSuspensao() == null) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "Gravíssima durante o regime de reincidência exige diasSuspensao (10 a 90)");
        }

        Advertencia advertencia = new Advertencia();
        advertencia.setJogador(jogadorRepository.getReferenceById(jogadorId));
        advertencia.setNivel(nivel);
        advertencia.setStatus(StatusAdvertencia.ATIVA);
        advertencia.setFato(requisicao.fato());
        advertencia.setArtigo(requisicao.artigo());
        advertencia.setFundamentacao(requisicao.fundamentacao());
        advertencia.setMultiplicador(multiplicador);
        advertencia.setMultaFinal(calcularMulta(nivel, multiplicador));
        advertencia.setOficializadaEm(agora);
        advertencia.setValidaAte(agora.plusDays(nivel.getValidadeDias()));
        advertencia.setPrazoRecursoAte(agora.plusHours(HORAS_PRAZO_RECURSO));
        advertencia.setAplicadaPorId(aplicadorId);
        advertenciaRepository.save(advertencia);

        int partidas = requisicao.partidasSuspensao() == null ? PARTIDAS_PADRAO : requisicao.partidasSuspensao();

        if (suspensaoImediata) {
            if (nivel == NivelAdvertencia.GRAVISSIMA) {
                criarSuspensao(jogadorId, TipoSuspensaoAdvertencia.DIAS, requisicao.diasSuspensao(),
                        OrigemSuspensaoAdvertencia.REINCIDENCIA, List.of(advertencia), agora);
            } else {
                criarSuspensao(jogadorId, TipoSuspensaoAdvertencia.PARTIDAS, partidas,
                        OrigemSuspensaoAdvertencia.REINCIDENCIA, List.of(advertencia), agora);
                advertencia.setStatus(StatusAdvertencia.CONSUMIDA);
            }
        } else {
            converterPorContagem(jogadorId, nivel, agora, partidas);
        }

        return AdvertenciaPublicaDTO.de(advertencia);
    }

    @Transactional
    public AdvertenciaPublicaDTO cancelar(String advertenciaId, String cancelanteId, String motivo) {
        String jogadorId = buscarJogadorIdDaAdvertencia(advertenciaId);
        travarJogador(jogadorId);
        Advertencia advertencia = buscarAdvertencia(advertenciaId);

        Cargo cargoCancelante = jogadorRepository.buscarCargo(cancelanteId)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Usuário não encontrado"));
        validarPermissao(cargoCancelante, advertencia.getNivel());

        cancelarInterno(advertencia, jogadorId, cancelanteId, motivo, RelogioBrasilia.agora());
        return AdvertenciaPublicaDTO.de(advertencia);
    }

    @Transactional
    public AdvertenciaPublicaDTO interporRecurso(String advertenciaId, String solicitanteId, String texto) {
        String jogadorId = buscarJogadorIdDaAdvertencia(advertenciaId);
        travarJogador(jogadorId);
        Advertencia advertencia = buscarAdvertencia(advertenciaId);

        if (!jogadorId.equals(solicitanteId)) {
            throw erro(HttpStatus.FORBIDDEN, "Apenas o jogador advertido pode interpor recurso");
        }
        if (advertencia.getStatus() == StatusAdvertencia.CANCELADA) {
            throw erro(HttpStatus.CONFLICT, "Advertência já cancelada");
        }
        if (advertencia.isRecursoInterposto()) {
            throw erro(HttpStatus.CONFLICT, "Recurso já interposto");
        }

        OffsetDateTime agora = RelogioBrasilia.agora();
        if (!agora.isBefore(advertencia.getPrazoRecursoAte())) {
            throw erro(HttpStatus.CONFLICT, "Prazo de recurso encerrado");
        }

        advertencia.setRecursoInterposto(true);
        advertencia.setRecursoTexto(texto);
        advertencia.setRecursoEm(agora);
        return AdvertenciaPublicaDTO.de(advertencia);
    }

    @Transactional
    public AdvertenciaPublicaDTO decidirRecurso(String advertenciaId, String decisorId, DecidirRecursoRequest requisicao) {
        String jogadorId = buscarJogadorIdDaAdvertencia(advertenciaId);
        travarJogador(jogadorId);
        Advertencia advertencia = buscarAdvertencia(advertenciaId);

        Cargo cargoDecisor = jogadorRepository.buscarCargo(decisorId)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Usuário não encontrado"));
        validarPermissao(cargoDecisor, advertencia.getNivel());

        if (!advertencia.isRecursoInterposto()) {
            throw erro(HttpStatus.CONFLICT, "Não há recurso interposto");
        }
        if (advertencia.getRecursoDecididoEm() != null) {
            throw erro(HttpStatus.CONFLICT, "Recurso já decidido");
        }
        if (advertencia.getStatus() == StatusAdvertencia.CANCELADA) {
            throw erro(HttpStatus.CONFLICT, "Advertência já cancelada");
        }

        OffsetDateTime agora = RelogioBrasilia.agora();
        ResultadoRecurso resultado = requisicao.resultado();

        if (resultado == ResultadoRecurso.REDUZIDO) {
            Integer novoMultiplicador = requisicao.novoMultiplicador();
            if (novoMultiplicador == null || novoMultiplicador < 1 || novoMultiplicador >= advertencia.getMultiplicador()) {
                throw erro(HttpStatus.BAD_REQUEST, "novoMultiplicador deve ser menor que o atual e no mínimo 1");
            }
            advertencia.setMultiplicador(novoMultiplicador);
            advertencia.setMultaFinal(calcularMulta(advertencia.getNivel(), novoMultiplicador));
        }

        advertencia.setRecursoResultado(resultado);
        advertencia.setRecursoFundamentacao(requisicao.fundamentacao());
        advertencia.setRecursoDecididoEm(agora);

        if (resultado == ResultadoRecurso.ACOLHIDO) {
            cancelarInterno(advertencia, jogadorId, decisorId,
                    "Recurso acolhido: " + requisicao.fundamentacao(), agora);
        }

        return AdvertenciaPublicaDTO.de(advertencia);
    }

    @Transactional
    public void debitarMulta(String advertenciaId) {
        Optional<String> jogadorIdOpcional = advertenciaRepository.buscarJogadorId(advertenciaId);
        if (jogadorIdOpcional.isEmpty()) {
            return;
        }
        String jogadorId = jogadorIdOpcional.get();
        travarJogador(jogadorId);

        Optional<Advertencia> advertenciaOpcional = advertenciaRepository.findById(advertenciaId);
        OffsetDateTime agora = RelogioBrasilia.agora();
        if (advertenciaOpcional.isEmpty() || !multaDevida(advertenciaOpcional.get(), agora)) {
            return;
        }

        Advertencia advertencia = advertenciaOpcional.get();
        BigDecimal multa = advertencia.getMultaFinal();
        jogadorRepository.ajustarSaldo(jogadorId, multa.negate());

        BigDecimal saldoAtual = jogadorRepository.buscarSaldo(jogadorId).orElse(BigDecimal.ZERO);
        if (saldoAtual.signum() < 0 && !jogadorRepository.estaNegativado(jogadorId)) {
            jogadorRepository.marcarNegativado(jogadorId, RelogioBrasilia.agoraLocal());
            advertencia.setNegativouJogador(true);
        }

        advertencia.setMultaDebitada(true);
        advertencia.setValorDebitado(multa);
        advertencia.setDebitadaEm(agora);
    }

    @Transactional(readOnly = true)
    public FichaAdvertenciasDTO buscarFicha(String jogadorId) {
        String nome = jogadorRepository.buscarNome(jogadorId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Jogador não encontrado"));
        OffsetDateTime agora = RelogioBrasilia.agora();

        Map<NivelAdvertencia, Long> totais = new EnumMap<>(NivelAdvertencia.class);
        for (ContagemNivelProjecao projecao : advertenciaRepository.contarAtivasPorNivel(
                jogadorId, StatusAdvertencia.ATIVA, agora)) {
            totais.put(projecao.nivel(), projecao.total());
        }

        List<ContagemNivelDTO> contagens = new ArrayList<>();
        for (NivelAdvertencia nivel : NivelAdvertencia.values()) {
            contagens.add(new ContagemNivelDTO(nivel, totais.getOrDefault(nivel, 0L).intValue(),
                    nivel.getLimiteConversao()));
        }

        Optional<OffsetDateTime> fimRegime = advertenciaRepository.buscarFimDoRegime(
                jogadorId, NivelAdvertencia.GRAVISSIMA, StatusAdvertencia.ATIVA, agora);

        return new FichaAdvertenciasDTO(
                jogadorId,
                nome,
                fimRegime.isPresent(),
                RelogioBrasilia.paraBrasilia(fimRegime.orElse(null)),
                contagens,
                advertenciaRepository.buscarAtivasPublicas(jogadorId, StatusAdvertencia.ATIVA, agora)
        );
    }

    @Transactional(readOnly = true)
    public Page<AdvertenciaPublicaDTO> listarHistorico(String jogadorId, Pageable paginacao) {
        jogadorRepository.buscarNome(jogadorId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Jogador não encontrado"));
        return advertenciaRepository.buscarHistoricoPublico(jogadorId, paginacao);
    }

    private void cancelarInterno(Advertencia advertencia, String jogadorId, String cancelanteId,
                                 String motivo, OffsetDateTime agora) {
        if (advertencia.getStatus() == StatusAdvertencia.CANCELADA) {
            throw erro(HttpStatus.CONFLICT, "Advertência já cancelada");
        }

        Set<NivelAdvertencia> niveisRestaurados = EnumSet.noneOf(NivelAdvertencia.class);
        SuspensaoPorAdvertencia suspensao = advertencia.getSuspensao();
        if (suspensao != null && !suspensao.isCancelada()) {
            niveisRestaurados.addAll(desfazerSuspensao(advertencia, suspensao, jogadorId, agora));
        }

        BigDecimal valorDebitado = advertencia.getValorDebitado();
        if (valorDebitado != null && valorDebitado.signum() > 0) {
            devolverValor(jogadorId, advertencia, valorDebitado);
            advertencia.setValorDebitado(BigDecimal.ZERO);
        }

        advertencia.setStatus(StatusAdvertencia.CANCELADA);
        advertencia.setCanceladaEm(agora);
        advertencia.setCanceladaPorId(cancelanteId);
        advertencia.setMotivoCancelamento(motivo);

        for (NivelAdvertencia nivel : niveisRestaurados) {
            converterPorContagem(jogadorId, nivel, agora, PARTIDAS_PADRAO);
        }
    }

    private Set<NivelAdvertencia> desfazerSuspensao(Advertencia cancelada, SuspensaoPorAdvertencia suspensao,
                                                    String jogadorId, OffsetDateTime agora) {
        suspensao.setCancelada(true);
        suspensao.setCanceladaEm(agora);

        Set<NivelAdvertencia> niveisRestaurados = EnumSet.noneOf(NivelAdvertencia.class);
        for (Advertencia vinculada : advertenciaRepository.findBySuspensaoId(suspensao.getId())) {
            if (vinculada.getId().equals(cancelada.getId())) {
                continue;
            }
            vinculada.setSuspensao(null);
            if (vinculada.getStatus() == StatusAdvertencia.CONSUMIDA) {
                vinculada.setStatus(StatusAdvertencia.ATIVA);
                niveisRestaurados.add(vinculada.getNivel());
            }
        }

        publicador.publishEvent(new SuspensaoPorAdvertenciaCanceladaEvent(suspensao.getId(), jogadorId));
        return niveisRestaurados;
    }

    private void devolverValor(String jogadorId, Advertencia advertencia, BigDecimal valor) {
        jogadorRepository.ajustarSaldo(jogadorId, valor);
        if (advertencia.isNegativouJogador()) {
            BigDecimal saldoAtual = jogadorRepository.buscarSaldo(jogadorId).orElse(BigDecimal.ZERO);
            if (saldoAtual.signum() >= 0) {
                jogadorRepository.reverterNegativado(jogadorId, RelogioBrasilia.agoraLocal());
                advertencia.setNegativouJogador(false);
            }
        }
    }

    private void converterPorContagem(String jogadorId, NivelAdvertencia nivel, OffsetDateTime agora, int partidas) {
        if (!nivel.convertePorContagem()) {
            return;
        }

        List<Advertencia> ativas = advertenciaRepository
                .findByJogadorIdAndNivelAndStatusAndValidaAteAfterOrderByOficializadaEmAsc(
                        jogadorId, nivel, StatusAdvertencia.ATIVA, agora);
        int limite = nivel.getLimiteConversao();
        if (ativas.size() < limite) {
            return;
        }

        List<Advertencia> usadas = new ArrayList<>(ativas.subList(0, limite));
        criarSuspensao(jogadorId, TipoSuspensaoAdvertencia.PARTIDAS, partidas,
                OrigemSuspensaoAdvertencia.CONTAGEM, usadas, agora);
        for (Advertencia usada : usadas) {
            usada.setStatus(StatusAdvertencia.CONSUMIDA);
        }
    }

    private SuspensaoPorAdvertencia criarSuspensao(String jogadorId, TipoSuspensaoAdvertencia tipo, int quantidade,
                                                   OrigemSuspensaoAdvertencia origem, List<Advertencia> geradoras,
                                                   OffsetDateTime agora) {
        SuspensaoPorAdvertencia suspensao = new SuspensaoPorAdvertencia();
        suspensao.setJogador(jogadorRepository.getReferenceById(jogadorId));
        suspensao.setTipo(tipo);
        suspensao.setQuantidade(quantidade);
        suspensao.setOrigem(origem);
        suspensao.setCriadaEm(agora);
        suspensaoRepository.save(suspensao);

        List<String> advertenciaIds = new ArrayList<>();
        for (Advertencia geradora : geradoras) {
            geradora.setSuspensao(suspensao);
            advertenciaIds.add(geradora.getId());
        }

        publicador.publishEvent(new SuspensaoPorAdvertenciaGeradaEvent(
                suspensao.getId(), jogadorId, tipo, quantidade, origem, advertenciaIds));
        return suspensao;
    }

    private boolean multaDevida(Advertencia advertencia, OffsetDateTime agora) {
        if (advertencia.isMultaDebitada() || advertencia.getStatus() == StatusAdvertencia.CANCELADA) {
            return false;
        }
        if (!advertencia.getPrazoRecursoAte().isBefore(agora)) {
            return false;
        }
        return !advertencia.isRecursoInterposto() || advertencia.getRecursoDecididoEm() != null;
    }

    private BigDecimal calcularMulta(NivelAdvertencia nivel, int multiplicador) {
        return nivel.getMultaBase().multiply(BigDecimal.valueOf(multiplicador));
    }

    private void validarPermissao(Cargo cargo, NivelAdvertencia nivel) {
        boolean permitido;
        if (nivel == NivelAdvertencia.GRAVISSIMA) {
            permitido = cargo == Cargo.PROPRIETARIO;
        } else {
            permitido = cargo == Cargo.PROPRIETARIO || cargo == Cargo.DIRETOR;
        }
        if (!permitido) {
            throw erro(HttpStatus.FORBIDDEN, "Cargo sem permissão para este nível de advertência");
        }
    }

    private void validarMultiplicador(int multiplicador, String fundamentacao, Cargo cargoAplicador) {
        if (multiplicador > MULTIPLICADOR_PADRAO && (fundamentacao == null || fundamentacao.isBlank())) {
            throw erro(HttpStatus.BAD_REQUEST, "Multiplicador acima de x1 exige fundamentação escrita");
        }
        if (multiplicador > MULTIPLICADOR_MAXIMO_SUPERINTENDENCIA && cargoAplicador != Cargo.PROPRIETARIO) {
            throw erro(HttpStatus.FORBIDDEN, "Multiplicador acima de x10 só pode ser decidido pelo plenário");
        }
    }

    private void travarJogador(String jogadorId) {
        jogadorRepository.travarPorId(jogadorId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Jogador não encontrado"));
    }

    private String buscarJogadorIdDaAdvertencia(String advertenciaId) {
        return advertenciaRepository.buscarJogadorId(advertenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Advertência não encontrada"));
    }

    private Advertencia buscarAdvertencia(String advertenciaId) {
        return advertenciaRepository.findById(advertenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Advertência não encontrada"));
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
