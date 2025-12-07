package com.ddo.torneios.service;

import com.ddo.torneios.exception.TemporadaJaExisteException;
import com.ddo.torneios.model.Temporada;
import com.ddo.torneios.repository.TemporadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemporadaService {

    @Autowired
    private TemporadaRepository temporadaRepository;

    public void criarTemporada(Temporada temporada) {
        if (temporadaRepository.existsTemporadaByNome(temporada.getNome())) {
            throw new TemporadaJaExisteException(temporada.getNome());
        }

        temporadaRepository.save(temporada);
    }
}
