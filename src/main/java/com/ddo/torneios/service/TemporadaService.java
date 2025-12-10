package com.ddo.torneios.service;

import com.ddo.torneios.exception.TemporadaJaExisteException;
import com.ddo.torneios.model.Temporada;
import com.ddo.torneios.repository.TemporadaRepository;
import com.ddo.torneios.request.TemporadaRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemporadaService {

    @Autowired
    private TemporadaRepository temporadaRepository;

    public void criarTemporada(TemporadaRequest request) {
        if (temporadaRepository.existsTemporadaByNome(request.getNome())) {
            throw new TemporadaJaExisteException(request.getNome());
        }

        Temporada temporada = new Temporada(request.getNome(), request.getDataInicio(), request.getDataFim());
        temporadaRepository.save(temporada);
    }
}
