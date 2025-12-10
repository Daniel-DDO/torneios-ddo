package com.ddo.torneios.service;

import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParticipacaoFaseService {

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository;
}
