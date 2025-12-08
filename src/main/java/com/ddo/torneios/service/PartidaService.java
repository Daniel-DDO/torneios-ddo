package com.ddo.torneios.service;

import com.ddo.torneios.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;
}
