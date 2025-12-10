package com.ddo.torneios.service;

import com.ddo.torneios.dto.FaseTorneioDTO;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.Torneio;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.TorneioRepository;
import com.ddo.torneios.request.FaseTorneioRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FaseTorneioService {

    @Autowired
    private FaseTorneioRepository faseTorneioRepository;

    @Autowired
    private TorneioRepository torneioRepository;

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
}