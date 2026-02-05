package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorClubeDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.JogadorClubeRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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
}