package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorMataMataRankingStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();
        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("Para Mata-Mata por ranking, o número de participantes deve ser PAR.");
        }

        List<ParticipacaoFase> ranking = new ArrayList<>(participantes);
        ranking.sort(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao));

        List<Partida> partidas = new ArrayList<>();
        int totalConfrontos = n / 2;

        for (int i = 0; i < totalConfrontos; i++) {
            ParticipacaoFase favorito = ranking.get(i);
            ParticipacaoFase desafiante = ranking.get(n - 1 - i);

            partidas.addAll(criarConfronto(fase, i + 1, favorito, desafiante, "Ranking"));
        }

        return partidas;
    }
}