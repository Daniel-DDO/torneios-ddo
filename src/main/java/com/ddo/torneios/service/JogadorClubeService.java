package com.ddo.torneios.service;

import com.ddo.torneios.repository.JogadorClubeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JogadorClubeService {

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;
}
