package com.ddo.torneios.service;

import com.ddo.torneios.repository.TemporadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TemporadaService {

    @Autowired
    private TemporadaRepository temporadaRepository;
}
