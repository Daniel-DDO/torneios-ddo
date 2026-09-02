package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.RegraNegocioException;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.ConfirmacaoSorteioRequest;
import com.ddo.torneios.request.JogadorClubeRequest;
import com.ddo.torneios.request.SorteioRequest;
import com.ddo.torneios.request.TrocarJogadorClubePartidaRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class JogadorClubeService {

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private ClubeRepository clubeRepository;

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Autowired
    private TorneioRepository torneioRepository;

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private FaseTorneioRepository faseTorneioRepository;


    @Transactional
    public TrocaJogadorClubePartidaResultadoDTO trocarJogadorNaPartida(TrocarJogadorClubePartidaRequest request) {

        PartidaTrocaProjection partidaAtual = partidaRepository.buscarParaTroca(request.partidaId())
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + request.partidaId()));

        if (partidaAtual.realizada()) {
            throw new RegraNegocioException("Não é possível trocar jogador de uma partida já realizada.");
        }

        boolean antigoEhMandante = request.jogadorClubeAntigoId().equals(partidaAtual.mandanteId());
        boolean antigoEhVisitante = request.jogadorClubeAntigoId().equals(partidaAtual.visitanteId());

        if (!antigoEhMandante && !antigoEhVisitante) {
            throw new RegraNegocioException("O jogador informado não participa desta partida.");
        }

        JogadorClubeBaseDTO antigoJC = jogadorClubeRepository.buscarBasePorId(request.jogadorClubeAntigoId())
                .orElseThrow(() -> new EntityNotFoundException("Inscrição (JogadorClube) antiga não encontrada."));

        boolean[] jogadorClubeCriado = new boolean[1];
        JogadorClube novoJC = jogadorClubeRepository.findByJogadorIdAndTemporadaId(request.novoJogadorId(), antigoJC.temporadaId())
                .orElseGet(() -> {
                    jogadorClubeCriado[0] = true;
                    return criarNovoJogadorClube(request.novoJogadorId(), antigoJC);
                });

        boolean participacaoCriada = garantirParticipacaoFase(partidaAtual.faseId(), novoJC);

        List<String> partidasAtualizadas = new ArrayList<>();
        atualizarLadoDaPartida(partidaAtual.id(), antigoEhMandante, novoJC);
        partidasAtualizadas.add(partidaAtual.id());

        if (ehTipoIdaOuVolta(partidaAtual.tipoPartida())) {
            resolverPartidaPar(partidaAtual).ifPresent(par -> {
                if (par.realizada()) return;

                boolean antigoEhMandanteNoPar = request.jogadorClubeAntigoId().equals(par.mandanteId());
                boolean antigoEhVisitanteNoPar = request.jogadorClubeAntigoId().equals(par.visitanteId());

                if (antigoEhMandanteNoPar || antigoEhVisitanteNoPar) {
                    atualizarLadoDaPartida(par.id(), antigoEhMandanteNoPar, novoJC);
                    partidasAtualizadas.add(par.id());
                }
            });
        }

        return new TrocaJogadorClubePartidaResultadoDTO(
                novoJC.getId(), jogadorClubeCriado[0], participacaoCriada, partidasAtualizadas
        );
    }

    private JogadorClube criarNovoJogadorClube(String novoJogadorId, JogadorClubeBaseDTO antigoJC) {
        JogadorClube novo = new JogadorClube();
        novo.setJogador(jogadorRepository.getReferenceById(novoJogadorId));
        novo.setClube(clubeRepository.getReferenceById(antigoJC.clubeId()));
        novo.setTemporada(temporadaRepository.getReferenceById(antigoJC.temporadaId()));
        novo.setBalancoFinanceiro(BigDecimal.ZERO);
        novo.setPontosCoeficiente(BigDecimal.ZERO);
        novo.setTotalGolsMarcados(0);
        novo.setTotalGolsSofridos(0);
        novo.setPartidasJogadas(0);
        novo.setVitorias(0);
        novo.setEmpates(0);
        novo.setDerrotas(0);
        novo.setAproveitamento(0.0);
        novo.setStatusTemporada(StatusClassificacao.ATIVO);
        return jogadorClubeRepository.save(novo);
    }

    private boolean garantirParticipacaoFase(String faseId, JogadorClube jogadorClube) {
        if (participacaoFaseRepository.existsByFaseIdAndJogadorClubeId(faseId, jogadorClube.getId())) {
            return false;
        }
        ParticipacaoFase participacao = new ParticipacaoFase();
        participacao.setFase(faseTorneioRepository.getReferenceById(faseId));
        participacao.setJogadorClube(jogadorClube);
        participacao.setPontos(0);
        participacao.setPartidasJogadas(0);
        participacao.setVitorias(0);
        participacao.setEmpates(0);
        participacao.setDerrotas(0);
        participacao.setGolsPro(0);
        participacao.setGolsContra(0);
        participacao.setSaldoGols(0);
        participacao.setStatusClassificacao(jogadorClube.getStatusTemporada());
        participacaoFaseRepository.save(participacao);
        return true;
    }

    private void atualizarLadoDaPartida(String partidaId, boolean isMandante, JogadorClube novoJC) {
        if (isMandante) {
            partidaRepository.atualizarMandante(partidaId, novoJC);
        } else {
            partidaRepository.atualizarVisitante(partidaId, novoJC);
        }
    }

    private boolean ehTipoIdaOuVolta(TipoPartida tipo) {
        return tipo == TipoPartida.MATA_MATA_IDA || tipo == TipoPartida.MATA_MATA_VOLTA
                || tipo == TipoPartida.FINAL_IDA || tipo == TipoPartida.FINAL_VOLTA;
    }

    private Optional<PartidaTrocaProjection> resolverPartidaPar(PartidaTrocaProjection partida) {
        boolean ehVolta = partida.tipoPartida() == TipoPartida.MATA_MATA_VOLTA
                || partida.tipoPartida() == TipoPartida.FINAL_VOLTA;

        if (ehVolta) {
            return partidaRepository.buscarPartidaIdaPorProxima(partida.id());
        }
        if (partida.proximaPartidaId() == null) {
            return Optional.empty();
        }
        return partidaRepository.buscarParaTroca(partida.proximaPartidaId());
    }

    @Transactional
    public JogadorClubeDTO inscreverJogador(JogadorClubeRequest request) {
        if (jogadorClubeRepository.existsByJogadorIdAndTemporadaId(request.getJogadorId(), request.getTemporadaId())) {
            throw new IllegalArgumentException("Este jogador já está participando desta temporada com outro time.");
        }

        /*
        if (jogadorClubeRepository.existsByClubeIdAndTemporadaId(request.getClubeId(), request.getTemporadaId())) {
            throw new IllegalArgumentException("Este clube já foi escolhido por outro jogador nesta temporada.");
        }
         */

        Jogador jogador = jogadorRepository.findById(request.getJogadorId())
                .orElseThrow(() -> new EntityNotFoundException("Jogador não encontrado com ID: " + request.getJogadorId()));

        Clube clube = clubeRepository.findById(request.getClubeId())
                .orElseThrow(() -> new EntityNotFoundException("Clube não encontrado com ID: " + request.getClubeId()));

        Temporada temporada = temporadaRepository.findById(request.getTemporadaId())
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com ID: " + request.getTemporadaId()));

        JogadorClube inscricao = new JogadorClube();
        inscricao.setJogador(jogador);
        inscricao.setClube(clube);
        inscricao.setTemporada(temporada);

        inscricao.setBalancoFinanceiro(BigDecimal.ZERO);
        inscricao.setPontosCoeficiente(BigDecimal.ZERO);
        inscricao.setTotalGolsMarcados(0);
        inscricao.setTotalGolsSofridos(0);
        inscricao.setPartidasJogadas(0);
        inscricao.setVitorias(0);
        inscricao.setEmpates(0);
        inscricao.setDerrotas(0);
        inscricao.setAproveitamento(0.0);

        inscricao.setStatusTemporada(StatusClassificacao.ATIVO);

        jogadorClubeRepository.save(inscricao);

        return new JogadorClubeDTO(inscricao);
    }

    public List<JogadorClubeDTO> listarInscritosPorTemporada(String temporadaId) {
        return jogadorClubeRepository.findByTemporadaId(temporadaId).stream()
                .map(JogadorClubeDTO::new)
                .toList();
    }

    @Transactional
    public void removerInscricao(String id) {
        if (!jogadorClubeRepository.existsById(id)) {
            throw new EntityNotFoundException("Inscrição não encontrada com ID: " + id);
        }
        jogadorClubeRepository.deleteById(id);
    }

    public List<JogadorClubeInscritoDTO> listarInscritosResumoPorTemporada(String temporadaId) {
        return jogadorClubeRepository.buscarInscritosResumo(temporadaId);
    }

    public List<JogadorClubeDTO> listarTodos() {
        return jogadorClubeRepository.findAll().stream()
                .map(JogadorClubeDTO::new)
                .toList();
    }

    public List<JogadorClubeDTO> listarInscritosPorTorneio(String torneioId) {
        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new EntityNotFoundException("Torneio não encontrado com ID: " + torneioId));

        return listarInscritosPorTemporada(torneio.getTemporada().getId());
    }

    public List<JogadorClubeDTO> buscarAutocompletePorJogador(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }
        return jogadorClubeRepository.findTop10ByJogadorNomeContainingIgnoreCase(termo.trim())
                .stream()
                .map(JogadorClubeDTO::new)
                .toList();
    }

    public List<JogadorClubeDTO> buscarAutocompletePorClube(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }
        return jogadorClubeRepository.findTop10ByClubeNomeContainingIgnoreCase(termo.trim())
                .stream()
                .map(JogadorClubeDTO::new)
                .toList();
    }

    @Transactional
    public void substituirJogador(String idJogadorClubeAntigo, String idNovoJogador) {
        JogadorClube antigoJC = jogadorClubeRepository.findById(idJogadorClubeAntigo)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição antiga não encontrada."));

        Jogador novoJogador = jogadorRepository.findById(idNovoJogador)
                .orElseThrow(() -> new EntityNotFoundException("Novo jogador não encontrado."));

        if (jogadorClubeRepository.existsByJogadorIdAndTemporadaId(idNovoJogador, antigoJC.getTemporada().getId())) {
            throw new IllegalArgumentException("O novo jogador já está inscrito nesta temporada.");
        }

        JogadorClube novoJC = new JogadorClube();
        novoJC.setJogador(novoJogador);
        novoJC.setClube(antigoJC.getClube());
        novoJC.setTemporada(antigoJC.getTemporada());

        novoJC.setBalancoFinanceiro(BigDecimal.ZERO);
        novoJC.setPontosCoeficiente(BigDecimal.ZERO);
        novoJC.setTotalGolsMarcados(0);
        novoJC.setTotalGolsSofridos(0);
        novoJC.setPartidasJogadas(0);
        novoJC.setVitorias(0);
        novoJC.setEmpates(0);
        novoJC.setDerrotas(0);
        novoJC.setAproveitamento(0.0);

        novoJC.setStatusTemporada(StatusClassificacao.ATIVO);

        novoJC = jogadorClubeRepository.save(novoJC);

        List<ParticipacaoFase> participacoes = participacaoFaseRepository.findByJogadorClube(antigoJC);

        for (ParticipacaoFase participacao : participacoes) {
            participacao.setJogadorClube(novoJC);
            participacaoFaseRepository.save(participacao);
        }

        List<Partida> partidasPendentes = partidaRepository.findPartidasNaoRealizadasPorJogadorClube(antigoJC.getId());

        for (Partida partida : partidasPendentes) {
            boolean atualizado = false;

            if (partida.getMandante() != null && partida.getMandante().equals(antigoJC)) {
                partida.setMandante(novoJC);
                atualizado = true;
            }

            if (partida.getVisitante() != null && partida.getVisitante().equals(antigoJC)) {
                partida.setVisitante(novoJC);
                atualizado = true;
            }

            if (atualizado) {
                partidaRepository.save(partida);
            }
        }

        antigoJC.setStatusTemporada(StatusClassificacao.SUBSTITUIDO);
        antigoJC.setIdDeQuemMeSubstituiu(novoJC.getId());
        jogadorClubeRepository.save(antigoJC);
    }

    @Transactional
    public void trocarClube(String idJogadorClube, String idNovoClube) {
        JogadorClube jogadorClube = jogadorClubeRepository.findById(idJogadorClube)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada."));

        Clube novoClube = clubeRepository.findById(idNovoClube)
                .orElseThrow(() -> new EntityNotFoundException("Clube não encontrado."));

        jogadorClube.setClube(novoClube);
        jogadorClubeRepository.save(jogadorClube);
    }

    public List<JogadorClubeDTO> buscarAutocompleteNaTemporada(String termo, String temporadaId) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }

        PageRequest limit = PageRequest.of(0, 10);

        return jogadorClubeRepository.buscarPorTermoETemporada(termo.trim(), temporadaId, limit)
                .stream()
                .map(JogadorClubeDTO::new)
                .toList();
    }

    @Transactional
    public List<JogadorClubeDTO> realizarSorteio(SorteioRequest request) {
        if (request.getClubesIds().size() < request.getJogadoresIds().size()) {
            throw new RegraNegocioException("A quantidade de clubes deve ser maior ou igual à de jogadores.");
        }

        Temporada temporada = temporadaRepository.findById(request.getTemporadaId())
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada."));

        List<Jogador> jogadores = jogadorRepository.findAllById(request.getJogadoresIds());
        List<Clube> clubes = clubeRepository.findAllById(request.getClubesIds());

        if (jogadores.size() != request.getJogadoresIds().size()) {
            throw new RegraNegocioException("Alguns jogadores não foram encontrados.");
        }
        if (clubes.size() != request.getClubesIds().size()) {
            throw new RegraNegocioException("Alguns clubes não foram encontrados.");
        }

        List<Clube> clubesSorteio = new ArrayList<>(clubes);
        Collections.shuffle(clubesSorteio);

        List<JogadorClube> novasInscricoes = new ArrayList<>();

        for (int i = 0; i < jogadores.size(); i++) {
            Jogador jogador = jogadores.get(i);
            Clube clubeSorteado = clubesSorteio.get(i);

            if (jogadorClubeRepository.existsByJogadorIdAndTemporadaId(jogador.getId(), temporada.getId())) {
                throw new RegraNegocioException("O jogador " + jogador.getNome() + " já está inscrito nesta temporada.");
            }

            JogadorClube novaInscricao = new JogadorClube();
            novaInscricao.setJogador(jogador);
            novaInscricao.setClube(clubeSorteado);
            novaInscricao.setTemporada(temporada);

            novaInscricao.setBalancoFinanceiro(BigDecimal.ZERO);
            novaInscricao.setPontosCoeficiente(BigDecimal.ZERO);

            novasInscricoes.add(novaInscricao);
        }

        List<JogadorClube> salvos = jogadorClubeRepository.saveAll(novasInscricoes);

        return salvos.stream().map(JogadorClubeDTO::new).toList();
    }

    public List<SorteioResultadoDTO> simularSorteio(SorteioRequest request) {
        if (request.getClubesIds().size() < request.getJogadoresIds().size()) {
            throw new RegraNegocioException("Quantidade de clubes insuficiente para o número de jogadores.");
        }

        List<Jogador> jogadores = jogadorRepository.findAllById(request.getJogadoresIds());
        List<Clube> clubes = clubeRepository.findAllById(request.getClubesIds());

        if (jogadores.size() != request.getJogadoresIds().size() || clubes.size() != request.getClubesIds().size()) {
            throw new RegraNegocioException("Alguns jogadores ou clubes não foram encontrados.");
        }

        List<Clube> clubesSorteio = new ArrayList<>(clubes);
        Collections.shuffle(clubesSorteio);

        List<SorteioResultadoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < jogadores.size(); i++) {
            Jogador jogador = jogadores.get(i);
            Clube clube = clubesSorteio.get(i);

            resultado.add(new SorteioResultadoDTO(
                    jogador.getId(),
                    jogador.getNome(),
                    clube.getId(),
                    clube.getNome(),
                    clube.getImagem()
            ));
        }

        return resultado;
    }

    @Transactional
    public void confirmarInscricoesEmLote(ConfirmacaoSorteioRequest request) {
        Temporada temporada = temporadaRepository.findById(request.getTemporadaId())
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada."));

        List<JogadorClube> listaParaSalvar = new ArrayList<>();

        for (ConfirmacaoSorteioRequest.ParInscricao par : request.getInscricoes()) {
            if (jogadorClubeRepository.existsByJogadorIdAndTemporadaId(par.getJogadorId(), temporada.getId())) {
                throw new RegraNegocioException("O jogador com ID " + par.getJogadorId() + " já está inscrito nesta temporada.");
            }

            Jogador jogador = jogadorRepository.getReferenceById(par.getJogadorId());
            Clube clube = clubeRepository.getReferenceById(par.getClubeId());

            JogadorClube novo = new JogadorClube();
            novo.setJogador(jogador);
            novo.setClube(clube);
            novo.setTemporada(temporada);
            novo.setBalancoFinanceiro(BigDecimal.ZERO);
            novo.setPontosCoeficiente(BigDecimal.ZERO);
            listaParaSalvar.add(novo);
        }

        jogadorClubeRepository.saveAll(listaParaSalvar);
    }

    @Transactional
    public void substituirJogadorNoTorneio(String idJogadorClubeAntigo, String idNovoJogador, String torneioId) {
        JogadorClube antigoJC = jogadorClubeRepository.findById(idJogadorClubeAntigo)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição antiga não encontrada."));

        Torneio torneio = torneioRepository.findById(torneioId)
                .orElseThrow(() -> new EntityNotFoundException("Torneio não encontrado."));

        if (!antigoJC.getTemporada().getId().equals(torneio.getTemporada().getId())) {
            throw new RegraNegocioException("O jogador antigo não pertence à temporada deste torneio.");
        }

        List<String> fasesIds = torneio.getFases().stream().map(FaseTorneio::getId).toList();

        JogadorClube novoJC = jogadorClubeRepository.findByJogadorIdAndTemporadaId(idNovoJogador, antigoJC.getTemporada().getId())
                .map(existente -> {
                    boolean jaParticipaDoTorneio = participacaoFaseRepository.findByJogadorClube(existente).stream()
                            .anyMatch(p -> fasesIds.contains(p.getFase().getId()));

                    if (jaParticipaDoTorneio) {
                        throw new RegraNegocioException("Esse jogador já participa deste torneio.");
                    }

                    return existente;
                })
                .orElseGet(() -> {
                    Jogador novoJogador = jogadorRepository.findById(idNovoJogador)
                            .orElseThrow(() -> new EntityNotFoundException("Novo jogador não encontrado."));

                    JogadorClube criado = new JogadorClube();
                    criado.setJogador(novoJogador);
                    criado.setClube(antigoJC.getClube());
                    criado.setTemporada(antigoJC.getTemporada());
                    criado.setBalancoFinanceiro(BigDecimal.ZERO);
                    criado.setPontosCoeficiente(BigDecimal.ZERO);
                    criado.setTotalGolsMarcados(0);
                    criado.setTotalGolsSofridos(0);
                    criado.setPartidasJogadas(0);
                    criado.setVitorias(0);
                    criado.setEmpates(0);
                    criado.setDerrotas(0);
                    criado.setAproveitamento(0.0);
                    criado.setStatusTemporada(StatusClassificacao.ATIVO);

                    return jogadorClubeRepository.save(criado);
                });

        List<ParticipacaoFase> participacoes = participacaoFaseRepository.findByJogadorClube(antigoJC).stream()
                .filter(p -> fasesIds.contains(p.getFase().getId()))
                .toList();

        for (ParticipacaoFase participacao : participacoes) {
            participacao.getHistoricoJogadorClubeIds().add(antigoJC.getId());
            participacao.setJogadorClube(novoJC);
            participacaoFaseRepository.save(participacao);
        }

        List<Partida> partidasPendentes = partidaRepository.findPartidasNaoRealizadasPorJogadorClubeETorneio(antigoJC.getId(), torneioId);

        for (Partida partida : partidasPendentes) {
            boolean atualizado = false;

            if (partida.getMandante() != null && partida.getMandante().equals(antigoJC)) {
                partida.setMandante(novoJC);
                atualizado = true;
            }

            if (partida.getVisitante() != null && partida.getVisitante().equals(antigoJC)) {
                partida.setVisitante(novoJC);
                atualizado = true;
            }

            if (atualizado) {
                partidaRepository.save(partida);
            }
        }
    }

    @Transactional
    public void desfazerSubstituicao(String idJogadorClubeAntigo) {
        JogadorClube antigoJC = jogadorClubeRepository.findById(idJogadorClubeAntigo)
                .orElseThrow(() -> new EntityNotFoundException("Inscrição não encontrada."));

        if (antigoJC.getStatusTemporada() != StatusClassificacao.SUBSTITUIDO || antigoJC.getIdDeQuemMeSubstituiu() == null) {
            throw new RegraNegocioException("Esta inscrição não foi substituída, não há o que desfazer.");
        }

        JogadorClube novoJC = jogadorClubeRepository.findById(antigoJC.getIdDeQuemMeSubstituiu())
                .orElseThrow(() -> new EntityNotFoundException("Inscrição do jogador substituto não encontrada."));

        if (novoJC.getStatusTemporada() != StatusClassificacao.ATIVO) {
            throw new RegraNegocioException("O jogador substituto já não está mais ativo (provavelmente foi substituído por outro jogador). Não é possível desfazer automaticamente.");
        }

        List<ParticipacaoFase> participacoes = participacaoFaseRepository.findByJogadorClube(novoJC);

        for (ParticipacaoFase participacao : participacoes) {
            participacao.setJogadorClube(antigoJC);
            participacaoFaseRepository.save(participacao);
        }

        List<Partida> partidasPendentes = partidaRepository.findPartidasNaoRealizadasPorJogadorClube(novoJC.getId());

        for (Partida partida : partidasPendentes) {
            boolean atualizado = false;

            if (partida.getMandante() != null && partida.getMandante().equals(novoJC)) {
                partida.setMandante(antigoJC);
                atualizado = true;
            }

            if (partida.getVisitante() != null && partida.getVisitante().equals(novoJC)) {
                partida.setVisitante(antigoJC);
                atualizado = true;
            }

            if (atualizado) {
                partidaRepository.save(partida);
            }
        }

        antigoJC.setStatusTemporada(StatusClassificacao.ATIVO);
        antigoJC.setIdDeQuemMeSubstituiu(null);
        jogadorClubeRepository.save(antigoJC);

        boolean novoJogouAlgumaPartida = partidaRepository.existsPartidaRealizadaPorJogadorClube(novoJC.getId());

        if (novoJogouAlgumaPartida) {
            novoJC.setStatusTemporada(StatusClassificacao.SUBSTITUIDO);
            novoJC.setIdDeQuemMeSubstituiu(antigoJC.getId());
            jogadorClubeRepository.save(novoJC);
        } else {
            jogadorClubeRepository.delete(novoJC);
        }
    }
}