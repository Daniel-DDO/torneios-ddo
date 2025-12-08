package com.ddo.torneios.service;

import com.ddo.torneios.repository.InsigniaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InsigniaService {

    @Autowired
    private InsigniaRepository insigniaRepository;
}
