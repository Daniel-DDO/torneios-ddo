package com.ddo.torneios.service;

import com.ddo.torneios.dto.TorneioDTO;
import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.model.Temporada;
import com.ddo.torneios.model.Torneio;
import com.ddo.torneios.repository.CompeticaoRepository;
import com.ddo.torneios.repository.TemporadaRepository;
import com.ddo.torneios.repository.TorneioRepository;
import com.ddo.torneios.request.TorneioRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TorneioService {

    @Autowired
    private TorneioRepository torneioRepository;

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Autowired
    private CompeticaoRepository competicaoRepository;

    @Transactional
    public TorneioDTO criarTorneio(TorneioRequest request) {
        Temporada temporada = temporadaRepository.findById(request.getTemporadaId())
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com ID: " + request.getTemporadaId()));

        Competicao competicao = competicaoRepository.findById(request.getCompeticaoId())
                .orElseThrow(() -> new EntityNotFoundException("Competição não encontrada com ID: " + request.getCompeticaoId()));

        if (torneioRepository.existsByTemporadaIdAndCompeticaoId(request.getTemporadaId(), request.getCompeticaoId())) {
            throw new IllegalArgumentException("Esta competição já foi adicionada a esta temporada.");
        }

        Torneio torneio = new Torneio();
        torneio.setNome(request.getNome());
        torneio.setTemporada(temporada);
        torneio.setCompeticao(competicao);

        torneioRepository.save(torneio);

        return new TorneioDTO(torneio);
    }

    public List<TorneioDTO> listarTorneiosPorTemporada(String temporadaId) {
        return torneioRepository.findByTemporadaId(temporadaId).stream()
                .map(TorneioDTO::new)
                .collect(Collectors.toList());
    }

    public TorneioDTO buscarTorneioPorId(String id) {
        Torneio torneio = torneioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Torneio não encontrado com ID: " + id));
        return new TorneioDTO(torneio);
    }

    @Transactional
    public void deletarTorneio(String id) {
        if (!torneioRepository.existsById(id)) {
            throw new EntityNotFoundException("Torneio não encontrado com ID: " + id);
        }
        torneioRepository.deleteById(id);
    }

    public List<TorneioDTO> listarTodos() {
        return torneioRepository.findAll()
                .stream()
                .map(TorneioDTO::new)
                .toList();
    }

    public List<TorneioDTO> listarPorTemporada(String temporadaId) {
        return torneioRepository.findByTemporadaId(temporadaId)
                .stream()
                .map(TorneioDTO::new)
                .toList();
    }
}