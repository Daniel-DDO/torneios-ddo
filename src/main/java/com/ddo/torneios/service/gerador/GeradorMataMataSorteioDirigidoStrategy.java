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

        validarQuantidadeParticipantes(n, fase);

        List<ParticipacaoFase> ranking = new ArrayList<>(participantes);
        ranking.sort(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao, Comparator.nullsLast(Comparator.naturalOrder())));

        int totalConfrontos = n / 2;

        if (totalConfrontos % 2 != 0) {
            throw new IllegalArgumentException(
                    "Sorteio dirigido exige um total de participantes múltiplo de 4 (para formar quadrantes pares).");
        }

        int numQuadrantes = totalConfrontos / 2;

        List<ParticipacaoFase> poteA = new ArrayList<>(ranking.subList(0, totalConfrontos));
        List<ParticipacaoFase> poteB = new ArrayList<>(ranking.subList(totalConfrontos, n));

        List<ParticipacaoFase> cabecasDeChave = new ArrayList<>(poteA.subList(0, numQuadrantes));
        List<ParticipacaoFase> segundaLinha = new ArrayList<>(poteA.subList(numQuadrantes, totalConfrontos));

        Random random = new Random();

        ParticipacaoFase[] cabecaPorQuadrante = new ParticipacaoFase[numQuadrantes];

        List<Integer> quadrantesEsquerda = new ArrayList<>();
        List<Integer> quadrantesDireita = new ArrayList<>();
        for (int q = 0; q < numQuadrantes; q++) {
            if (q < numQuadrantes / 2) quadrantesEsquerda.add(q);
            else quadrantesDireita.add(q);
        }

        List<ParticipacaoFase> cabecasRestantes = new ArrayList<>(cabecasDeChave);

        if (cabecasRestantes.size() >= 2 && !quadrantesEsquerda.isEmpty() && !quadrantesDireita.isEmpty()) {
            ParticipacaoFase rank1 = cabecasRestantes.remove(0);
            ParticipacaoFase rank2 = cabecasRestantes.remove(0);

            boolean rank1NaEsquerda = random.nextBoolean();
            List<Integer> ladoRank1 = rank1NaEsquerda ? quadrantesEsquerda : quadrantesDireita;
            List<Integer> ladoRank2 = rank1NaEsquerda ? quadrantesDireita : quadrantesEsquerda;

            int qRank1 = ladoRank1.remove(random.nextInt(ladoRank1.size()));
            int qRank2 = ladoRank2.remove(random.nextInt(ladoRank2.size()));

            cabecaPorQuadrante[qRank1] = rank1;
            cabecaPorQuadrante[qRank2] = rank2;
        }

        List<Integer> quadrantesLivres = new ArrayList<>();
        for (int q = 0; q < numQuadrantes; q++) {
            if (cabecaPorQuadrante[q] == null) quadrantesLivres.add(q);
        }
        Collections.shuffle(quadrantesLivres);
        Collections.shuffle(cabecasRestantes);
        for (int i = 0; i < cabecasRestantes.size(); i++) {
            cabecaPorQuadrante[quadrantesLivres.get(i)] = cabecasRestantes.get(i);
        }

        Collections.shuffle(segundaLinha);
        ParticipacaoFase[] segundaPorQuadrante = new ParticipacaoFase[numQuadrantes];
        for (int q = 0; q < numQuadrantes; q++) {
            segundaPorQuadrante[q] = segundaLinha.get(q);
        }

        ParticipacaoFase[] representantesPoteA = new ParticipacaoFase[totalConfrontos];
        for (int q = 0; q < numQuadrantes; q++) {
            int posImpar = q * 2;
            int posPar = q * 2 + 1;
            representantesPoteA[posImpar] = segundaPorQuadrante[q];
            representantesPoteA[posPar] = cabecaPorQuadrante[q];
        }

        Collections.shuffle(poteB);
        Collections.shuffle(poteB);

        List<Partida> partidasGeradas = new ArrayList<>();
        FaseMataMata faseInicial = fase.getFaseInicialMataMata();
        String nomeFaseLog = faseInicial.name();

        for (int i = 0; i < totalConfrontos; i++) {
            List<Partida> confronto = criarConfronto(
                    fase,
                    i + 1,
                    representantesPoteA[i],
                    poteB.get(i),
                    nomeFaseLog
            );

            confronto.forEach(p -> p.setEtapaMataMata(faseInicial));
            partidasGeradas.addAll(confronto);
        }

        vincularProximasFases(partidasGeradas, fase);

        return partidasGeradas;
    }
}