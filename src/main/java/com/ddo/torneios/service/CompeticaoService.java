package com.ddo.torneios.service;

import com.ddo.torneios.exception.CompeticaoExisteException;
import com.ddo.torneios.model.Competicao;
import com.ddo.torneios.repository.CompeticaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompeticaoService {

    @Autowired
    private CompeticaoRepository competicaoRepository;

    public void criarCompeticao(Competicao competicao) {
        if (competicaoRepository.existsByNome(competicao.getNome())) {
            throw new CompeticaoExisteException(competicao.getNome());
        }

        competicaoRepository.save(competicao);
    }
}
