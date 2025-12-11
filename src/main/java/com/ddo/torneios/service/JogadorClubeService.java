package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorClubeDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.JogadorClubeRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional
    public JogadorClubeDTO inscreverJogador(JogadorClubeRequest request) {
        if (jogadorClubeRepository.existsByJogadorIdAndTemporadaId(request.getJogadorId(), request.getTemporadaId())) {
            throw new IllegalArgumentException("Este jogador já está participando desta temporada com outro time.");
        }

        if (jogadorClubeRepository.existsByClubeIdAndTemporadaId(request.getClubeId(), request.getTemporadaId())) {
            throw new IllegalArgumentException("Este clube já foi escolhido por outro jogador nesta temporada.");
        }

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
}