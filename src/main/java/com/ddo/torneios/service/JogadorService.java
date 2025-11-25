package com.ddo.torneios.service;

import com.ddo.torneios.exception.JogadorExisteException;
import com.ddo.torneios.model.Jogador;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.request.JogadorRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JogadorService {

    @Autowired
    private JogadorRepository jogadorRepository;

    public void cadastrarJogador(JogadorRequest request) {
        if (jogadorRepository.existsJogadorByDiscord(request.getDiscord())) {
            throw new JogadorExisteException(request.getDiscord());
        }

        Jogador jogador = new Jogador(request.getNome(), request.getDiscord());
        jogadorRepository.save(jogador);
    }

}
