package com.ddo.torneios.service;

import com.ddo.torneios.dto.ParticipacaoFaseDTO;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.request.AtualizarParticipacaoFaseRequest;
import com.ddo.torneios.request.ParticipacaoFaseRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class ParticipacaoFaseService {

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository;

    @Autowired
    private FaseTorneioRepository faseTorneioRepository;

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Transactional
    public ParticipacaoFaseDTO adicionarParticipante(ParticipacaoFaseRequest request) {
        if (participacaoFaseRepository.existsByFaseIdAndJogadorClubeId(request.getFaseId(), request.getJogadorClubeId())) {
            throw new IllegalArgumentException("Este jogador/clube já está participando desta fase.");
        }

        FaseTorneio fase = faseTorneioRepository.findById(request.getFaseId())
                .orElseThrow(() -> new EntityNotFoundException("Fase não encontrada com ID: " + request.getFaseId()));

        JogadorClube jogadorClube = jogadorClubeRepository.findById(request.getJogadorClubeId())
                .orElseThrow(() -> new EntityNotFoundException("Inscrição (Jogador/Clube) não encontrada com ID: " + request.getJogadorClubeId()));

        String temporadaTorneio = fase.getTorneio().getTemporada().getId();
        String temporadaJogador = jogadorClube.getTemporada().getId();

        if (!temporadaTorneio.equals(temporadaJogador)) {
            throw new IllegalArgumentException("O jogador está inscrito em uma temporada diferente da temporada deste torneio.");
        }

        ParticipacaoFase participacao = new ParticipacaoFase();
        participacao.setFase(fase);
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

        return new ParticipacaoFaseDTO(participacao);
    }

    public List<ParticipacaoFaseDTO> listarClassificacao(String faseId) {
        if (!faseTorneioRepository.existsById(faseId)) {
            throw new EntityNotFoundException("Fase não encontrada com ID: " + faseId);
        }

        return participacaoFaseRepository.findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(faseId)
                .stream()
                .map(ParticipacaoFaseDTO::new)
                .toList();
    }

    @Transactional
    public void removerParticipante(String id) {
        if (!participacaoFaseRepository.existsById(id)) {
            throw new EntityNotFoundException("Participação não encontrada com ID: " + id);
        }
        participacaoFaseRepository.deleteById(id);
    }

    public List<ParticipacaoFaseDTO> listarTodos() {
        return participacaoFaseRepository.findAll().stream()
                .map(ParticipacaoFaseDTO::new)
                .toList();
    }

    public ParticipacaoFaseDTO buscarPorId(String id) {
        ParticipacaoFase participacao = participacaoFaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada com ID: " + id));
        return new ParticipacaoFaseDTO(participacao);
    }

    public List<ParticipacaoFaseDTO> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }

        PageRequest limit = PageRequest.of(0, 10);

        return participacaoFaseRepository.buscarTop10PorNomeOuDiscord(termo.trim(), limit)
                .stream()
                .map(ParticipacaoFaseDTO::new)
                .toList();
    }

    @Transactional
    public ParticipacaoFaseDTO editarParticipante(String id, AtualizarParticipacaoFaseRequest request) {
        ParticipacaoFase participacao = participacaoFaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada com ID: " + id));

        if (request.pontos() != null) participacao.setPontos(request.pontos());
        if (request.partidasJogadas() != null) participacao.setPartidasJogadas(request.partidasJogadas());
        if (request.vitorias() != null) participacao.setVitorias(request.vitorias());
        if (request.empates() != null) participacao.setEmpates(request.empates());
        if (request.derrotas() != null) participacao.setDerrotas(request.derrotas());
        if (request.golsPro() != null) participacao.setGolsPro(request.golsPro());
        if (request.golsContra() != null) participacao.setGolsContra(request.golsContra());

        if (request.golsPro() != null || request.golsContra() != null) {
            participacao.setSaldoGols(participacao.getGolsPro() - participacao.getGolsContra());
        }

        participacaoFaseRepository.save(participacao);

        return new ParticipacaoFaseDTO(participacao);
    }
}