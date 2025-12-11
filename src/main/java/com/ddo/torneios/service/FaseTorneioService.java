package com.ddo.torneios.service;

import com.ddo.torneios.dto.FaseTorneioDTO;
import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.dto.RodadaDTO;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.Rodada;
import com.ddo.torneios.model.Torneio;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.RodadaRepository;
import com.ddo.torneios.repository.TorneioRepository;
import com.ddo.torneios.request.FaseTorneioRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class FaseTorneioService {

    @Autowired
    private FaseTorneioRepository faseTorneioRepository;

    @Autowired
    private TorneioRepository torneioRepository;

    @Autowired
    private RodadaRepository rodadaRepository;

    @Autowired
    private PartidaRepository partidaRepository;

    @Transactional
    public FaseTorneioDTO criarFase(FaseTorneioRequest request) {
        Torneio torneio = torneioRepository.findById(request.getTorneioId())
                .orElseThrow(() -> new EntityNotFoundException("Torneio não encontrado com ID: " + request.getTorneioId()));

        if (faseTorneioRepository.existsByTorneioIdAndOrdem(request.getTorneioId(), request.getOrdem())) {
            throw new IllegalArgumentException("Já existe uma fase com a ordem " + request.getOrdem() + " neste torneio.");
        }

        FaseTorneio fase = new FaseTorneio();
        fase.setNome(request.getNome());
        fase.setOrdem(request.getOrdem());
        fase.setTorneio(torneio);
        fase.setTipoTorneio(request.getTipoTorneio());

        fase.setNumeroRodadas(request.getNumeroRodadas());
        fase.setFaseInicialMataMata(request.getFaseInicialMataMata());
        fase.setTemJogoVolta(request.getTemJogoVolta());
        fase.setAlgoritmoLiga(request.getAlgoritmoLiga());
        fase.setAlgoritmoMataMata(request.getAlgoritmoMataMata());
        fase.setMaxJogosEmCasa(request.getMaxJogosEmCasa());

        FaseTorneio faseSalva = faseTorneioRepository.save(fase);

        return new FaseTorneioDTO(faseSalva);
    }

    public List<FaseTorneioDTO> listarFasesDoTorneio(String torneioId) {
        if (!torneioRepository.existsById(torneioId)) {
            throw new EntityNotFoundException("Torneio não encontrado com ID: " + torneioId);
        }

        return faseTorneioRepository.findByTorneioIdOrderByOrdemAsc(torneioId).stream()
                .map(FaseTorneioDTO::new)
                .toList();
    }

    @Transactional
    public void deletarFase(String id) {
        if (!faseTorneioRepository.existsById(id)) {
            throw new EntityNotFoundException("Fase não encontrada com ID: " + id);
        }
        faseTorneioRepository.deleteById(id);
    }

    public List<FaseTorneioDTO> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 3) {
            return Collections.emptyList();
        }

        return faseTorneioRepository.findTop10ByNomeContainingIgnoreCase(termo.trim())
                .stream()
                .map(FaseTorneioDTO::new)
                .toList();
    }

    public List<RodadaDTO> buscarTabelaLiga(String faseId) {
        List<Rodada> rodadas = rodadaRepository.buscarTodasPorFaseComPartidas(faseId);

        return rodadas.stream()
                .map(RodadaDTO::new)
                .toList();
    }

    public List<PartidaDTO> buscarPartidasMataMata(String faseId) {
        List<Partida> partidas = partidaRepository.findByFaseIdOrderByDataHoraAsc(faseId);

        return partidas.stream()
                .map(PartidaDTO::new)
                .toList();
    }

    public List<PartidaDTO> buscarHistoricoJogador(String faseId, String jogadorClubeId) {
        List<Partida> partidas = partidaRepository.findByJogadorNaFase(faseId, jogadorClubeId);

        return partidas.stream()
                .map(PartidaDTO::new)
                .toList();
    }
}