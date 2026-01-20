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

        validarQuantidadeParticipantes(n, fase.getFaseInicialMataMata());

        List<ParticipacaoFase> ranking = new ArrayList<>(participantes);

        ranking.sort(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao, Comparator.nullsLast(Comparator.naturalOrder())));

        //DEBUG:
        System.out.println(">>> DEBUG MATA-MATA RANKING (Primeiros vs Últimos):");
        if (!ranking.isEmpty()) {
            ParticipacaoFase primeiro = ranking.get(0);
            ParticipacaoFase ultimo = ranking.get(n - 1);
            System.out.println("1º Lugar: " + (primeiro.getJogadorClube() != null ? primeiro.getJogadorClube().getJogador().getNome() : "N/A"));
            System.out.println("Último Lugar (" + n + "º): " + (ultimo.getJogadorClube() != null ? ultimo.getJogadorClube().getJogador().getNome() : "N/A"));
        }

        List<Partida> partidasGeradas = new ArrayList<>();
        int totalConfrontos = n / 2;

        FaseMataMata faseInicial = fase.getFaseInicialMataMata();
        String nomeFaseLog = faseInicial.name();

        for (int i = 0; i < totalConfrontos; i++) {
            ParticipacaoFase favorito = ranking.get(i);
            ParticipacaoFase desafiante = ranking.get(n - 1 - i);

            List<Partida> confronto = criarConfronto(
                    fase,
                    i + 1,
                    favorito,
                    desafiante,
                    nomeFaseLog
            );

            confronto.forEach(p -> p.setEtapaMataMata(faseInicial));

            partidasGeradas.addAll(confronto);
        }

        vincularProximasFases(partidasGeradas, fase);

        return partidasGeradas;
    }
}