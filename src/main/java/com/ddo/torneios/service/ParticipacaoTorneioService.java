package com.ddo.torneios.service;

import com.ddo.torneios.repository.ParticipacaoTorneioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParticipacaoTorneioService {

    @Autowired
    private ParticipacaoTorneioRepository participacaoTorneioRepository;
}
