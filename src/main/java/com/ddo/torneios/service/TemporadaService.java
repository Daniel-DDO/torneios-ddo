package com.ddo.torneios.service;

import com.ddo.torneios.dto.TemporadaDTO;
import com.ddo.torneios.exception.TemporadaJaExisteException;
import com.ddo.torneios.model.Temporada;
import com.ddo.torneios.repository.TemporadaRepository;
import com.ddo.torneios.request.TemporadaRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TemporadaService {

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Transactional
    public TemporadaDTO criarTemporada(TemporadaRequest request) {
        if (temporadaRepository.existsTemporadaByNome(request.getNome())) {
            throw new TemporadaJaExisteException(request.getNome());
        }

        Temporada temporada = new Temporada(request.getNome(), request.getDataInicio(), request.getDataFim());
        temporada.setAtiva(true);

        Temporada temporadaSalva = temporadaRepository.save(temporada);
        return new TemporadaDTO(temporadaSalva);
    }

    public List<TemporadaDTO> listarTodas() {
        return temporadaRepository.findAll()
                .stream()
                .map(TemporadaDTO::new)
                .collect(Collectors.toList());
    }

    public TemporadaDTO buscarPorId(String id) {
        Temporada temporada = temporadaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com ID: " + id));
        return new TemporadaDTO(temporada);
    }

    public TemporadaDTO buscarPorNome(String nome) {
        Temporada temporada = temporadaRepository.findByNome(nome)
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com o nome: " + nome));
        return new TemporadaDTO(temporada);
    }

    @Transactional
    public TemporadaDTO encerrarTemporada(String id) {
        Temporada temporada = temporadaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com ID: " + id));

        temporada.setAtiva(false);

        if (temporada.getDataFim() == null) {
            temporada.setDataFim(LocalDate.now());
        }

        Temporada temporadaAtualizada = temporadaRepository.save(temporada);
        return new TemporadaDTO(temporadaAtualizada);
    }

    @Transactional
    public TemporadaDTO atualizarTemporada(String id, TemporadaRequest request) {
        Temporada temporada = temporadaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Temporada não encontrada com ID: " + id));

        if (!temporada.getNome().equals(request.getNome()) && temporadaRepository.existsTemporadaByNome(request.getNome())) {
            throw new TemporadaJaExisteException(request.getNome());
        }

        temporada.setNome(request.getNome());
        temporada.setDataInicio(request.getDataInicio());
        temporada.setDataFim(request.getDataFim());

        Temporada temporadaSalva = temporadaRepository.save(temporada);
        return new TemporadaDTO(temporadaSalva);
    }

    @Transactional
    public void deletarTemporada(String id) {
        if (!temporadaRepository.existsById(id)) {
            throw new EntityNotFoundException("Temporada não encontrada com ID: " + id);
        }
        temporadaRepository.deleteById(id);
    }
}