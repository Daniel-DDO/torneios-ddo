package com.ddo.torneios.service;

import com.ddo.torneios.repository.RodadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RodadaService {

    @Autowired
    private RodadaRepository rodadaRepository;
}
