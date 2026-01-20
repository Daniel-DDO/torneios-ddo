package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorMataMataSorteioDirigidoStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();

        if (n < 4 || n % 2 != 0) {
            throw new IllegalArgumentException("Para sorteio dirigido, o número de participantes deve ser PAR e >= 4.");
        }

        validarQuantidadeParticipantes(n, fase.getFaseInicialMataMata());

        List<ParticipacaoFase> ranking = new ArrayList<>(participantes);
        ranking.sort(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao));

        int totalConfrontos = n / 2;
        int meioChave = totalConfrontos / 2;

        List<ParticipacaoFase> poteA = new ArrayList<>(ranking.subList(0, totalConfrontos));
        List<ParticipacaoFase> poteB = new ArrayList<>(ranking.subList(totalConfrontos, n));

        Collections.shuffle(poteB);

        ParticipacaoFase[] cabecasDeChave = new ParticipacaoFase[totalConfrontos];
        Random random = new Random();

        ParticipacaoFase rank1 = poteA.remove(0);
        ParticipacaoFase rank2 = poteA.remove(0);

        List<Integer> slotsParesEsquerda = gerarSlotsPares(1, meioChave);
        List<Integer> slotsParesDireita = gerarSlotsPares(meioChave + 1, totalConfrontos);

        boolean rank1NaEsquerda = random.nextBoolean();

        if (rank1NaEsquerda) {
            posicionarEmSlotAleatorio(cabecasDeChave, rank1, slotsParesEsquerda, random);
            posicionarEmSlotAleatorio(cabecasDeChave, rank2, slotsParesDireita, random);
        } else {
            posicionarEmSlotAleatorio(cabecasDeChave, rank1, slotsParesDireita, random);
            posicionarEmSlotAleatorio(cabecasDeChave, rank2, slotsParesEsquerda, random);
        }

        Collections.shuffle(poteA);
        Iterator<ParticipacaoFase> it = poteA.iterator();

        for (int i = 0; i < totalConfrontos; i++) {
            if (cabecasDeChave[i] == null) {
                cabecasDeChave[i] = it.next();
            }
        }

        List<Partida> partidasGeradas = new ArrayList<>();

        FaseMataMata faseInicial = fase.getFaseInicialMataMata();
        String nomeFaseLog = faseInicial.name();

        for (int i = 0; i < totalConfrontos; i++) {
            List<Partida> confronto = criarConfronto(
                    fase,
                    i + 1,
                    cabecasDeChave[i],
                    poteB.get(i),
                    nomeFaseLog
            );

            confronto.forEach(p -> p.setEtapaMataMata(faseInicial));

            partidasGeradas.addAll(confronto);
        }

        vincularProximasFases(partidasGeradas, fase);

        return partidasGeradas;
    }

    private void posicionarEmSlotAleatorio(ParticipacaoFase[] grid, ParticipacaoFase p, List<Integer> slots, Random r) {
        if (slots.isEmpty()) return;
        int escolhido = slots.get(r.nextInt(slots.size()));
        grid[escolhido - 1] = p;
    }

    private List<Integer> gerarSlotsPares(int inicio, int fim) {
        List<Integer> pares = new ArrayList<>();
        for (int i = inicio; i <= fim; i++) {
            if (i % 2 == 0) pares.add(i);
        }

        if (pares.isEmpty()) {
            for (int i = inicio; i <= fim; i++) pares.add(i);
        }
        return pares;
    }
}