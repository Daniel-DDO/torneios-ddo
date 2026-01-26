package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeradorMataMataPotesManuaisStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();

        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("Para Mata-Mata, o número total de participantes deve ser PAR.");
        }

        validarQuantidadeParticipantes(n, fase);

        List<ParticipacaoFase> poteA = new ArrayList<>();
        List<ParticipacaoFase> poteB = new ArrayList<>();
        List<ParticipacaoFase> semPote = new ArrayList<>();

        for (ParticipacaoFase p : participantes) {
            String grupo = p.getGrupo() != null ? p.getGrupo().trim() : "";

            if (grupo.equalsIgnoreCase("Pote A") || grupo.equalsIgnoreCase("A")) {
                poteA.add(p);
            } else if (grupo.equalsIgnoreCase("Pote B") || grupo.equalsIgnoreCase("B")) {
                poteB.add(p);
            } else {
                semPote.add(p);
            }
        }

        if (!semPote.isEmpty()) {
            throw new IllegalArgumentException("Existem participantes sem pote definido: " +
                    semPote.stream().map(p -> p.getJogadorClube().getJogador().getNome()).collect(Collectors.joining(", ")));
        }

        if (poteA.size() != poteB.size()) {
            throw new IllegalArgumentException(String.format("Os potes estão desbalanceados! Pote A: %d, Pote B: %d. Eles devem ter o mesmo tamanho.", poteA.size(), poteB.size()));
        }

        Collections.shuffle(poteA);
        Collections.shuffle(poteB);

        List<Partida> partidasGeradas = new ArrayList<>();
        FaseMataMata faseInicial = fase.getFaseInicialMataMata();
        String nomeFaseLog = faseInicial.name();

        int totalConfrontos = poteA.size();

        for (int i = 0; i < totalConfrontos; i++) {
            ParticipacaoFase timeA = poteA.get(i);
            ParticipacaoFase timeB = poteB.get(i);

            List<Partida> confronto = criarConfronto(
                    fase,
                    i + 1,
                    timeA,
                    timeB,
                    nomeFaseLog
            );

            confronto.forEach(p -> p.setEtapaMataMata(faseInicial));

            partidasGeradas.addAll(confronto);
        }

        vincularProximasFases(partidasGeradas, fase);

        return partidasGeradas;
    }
}