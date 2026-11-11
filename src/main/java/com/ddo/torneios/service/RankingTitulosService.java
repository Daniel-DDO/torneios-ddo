package com.ddo.torneios.service;

import com.ddo.torneios.dto.JogadorTitulosProjecao;
import com.ddo.torneios.dto.RankingTitulosDTO;
import com.ddo.torneios.repository.RankingTitulosRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingTitulosService {

    private static final int TAMANHO_RANKING = 10;

    private final RankingTitulosRepository rankingRepository;

    public RankingTitulosService(RankingTitulosRepository rankingRepository) {
        this.rankingRepository = rankingRepository;
    }

    @Transactional(readOnly = true)
    public List<RankingTitulosDTO> buscarTop10() {
        List<JogadorTitulosProjecao> jogadores = rankingRepository.buscarTopTitulos(PageRequest.of(0, TAMANHO_RANKING));
        List<RankingTitulosDTO> ranking = new ArrayList<>();
        int posicaoAtual = 0;
        int titulosAnterior = -1;

        for (int indice = 0; indice < jogadores.size(); indice++) {
            JogadorTitulosProjecao jogador = jogadores.get(indice);
            int titulos = jogador.titulos();
            if (titulos != titulosAnterior) {
                posicaoAtual = indice + 1;
                titulosAnterior = titulos;
            }
            int finais = jogador.finais() == null ? 0 : jogador.finais();
            ranking.add(new RankingTitulosDTO(posicaoAtual, jogador.id(), jogador.nome(), titulos, finais));
        }
        return ranking;
    }
}
