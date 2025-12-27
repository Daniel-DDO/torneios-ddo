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

        //rank 1 e rank 2 para posicionamento especial
        ParticipacaoFase rank1 = poteA.remove(0);
        ParticipacaoFase rank2 = poteA.remove(0);

        //slots pares (2, 4, 6, 8...) separados por metade superior e inferior
        List<Integer> slotsParesSuperior = gerarSlotsPares(1, meioChave);
        List<Integer> slotsParesInferior = gerarSlotsPares(meioChave + 1, totalConfrontos);

        boolean rank1NoSuperior = random.nextBoolean();
        if (rank1NoSuperior) {
            posicionarEmSlotAleatorio(cabecasDeChave, rank1, slotsParesSuperior, random);
            posicionarEmSlotAleatorio(cabecasDeChave, rank2, slotsParesInferior, random);
        } else {
            posicionarEmSlotAleatorio(cabecasDeChave, rank1, slotsParesInferior, random);
            posicionarEmSlotAleatorio(cabecasDeChave, rank2, slotsParesSuperior, random);
        }

        Collections.shuffle(poteA);
        Iterator<ParticipacaoFase> it = poteA.iterator();
        for (int i = 0; i < totalConfrontos; i++) {
            if (cabecasDeChave[i] == null) {
                cabecasDeChave[i] = it.next();
            }
        }

        List<Partida> partidas = new ArrayList<>();
        for (int i = 0; i < totalConfrontos; i++) {
            partidas.addAll(criarConfronto(fase, i + 1, cabecasDeChave[i], poteB.get(i), "Sorteio Dirigido"));
        }

        return partidas;
    }

    private void posicionarEmSlotAleatorio(ParticipacaoFase[] grid, ParticipacaoFase p, List<Integer> slots, Random r) {
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