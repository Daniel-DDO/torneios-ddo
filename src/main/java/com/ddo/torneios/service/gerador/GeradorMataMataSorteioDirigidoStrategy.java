package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorMataMataSorteioDirigidoStrategy implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {

        int n = participantes.size();
        if (n < 4 || n % 2 != 0) {
            throw new IllegalArgumentException("Para sorteio dirigido, número de participantes deve ser PAR e >= 4.");
        }
        validarQuantidadeVsEtapa(n, fase.getFaseInicialMataMata());

        if (participantes.stream().anyMatch(p -> p.getPosicaoClassificacao() == null)) {
            throw new IllegalStateException("Todos os participantes precisam ter 'posicaoClassificacao'.");
        }

        List<ParticipacaoFase> ranking = new ArrayList<>(participantes);
        ranking.sort(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao));

        int totalConfrontos = n / 2;
        int meioChave = totalConfrontos / 2;

        List<ParticipacaoFase> poteA = new ArrayList<>(ranking.subList(0, totalConfrontos)); //melhores
        List<ParticipacaoFase> poteB = new ArrayList<>(ranking.subList(totalConfrontos, n)); //piores
        Collections.shuffle(poteB);

        //array de cabeças de chave (indices 0 a totalConfrontos-1)
        ParticipacaoFase[] cabecasDeChave = new ParticipacaoFase[totalConfrontos];
        Random random = new Random();

        ParticipacaoFase rank1 = poteA.remove(0);
        ParticipacaoFase rank2 = poteA.remove(0);

        List<Integer> slotsParesSuperior = gerarSlotsPares(1, meioChave);
        List<Integer> slotsParesInferior = gerarSlotsPares(meioChave + 1, totalConfrontos);

        boolean rank1NoSuperior = random.nextBoolean();

        if (rank1NoSuperior) {
            //Rank 1 vai para cima (2 ou 4)
            int indexSlot1 = slotsParesSuperior.get(random.nextInt(slotsParesSuperior.size())) - 1; // -1 pois array é base 0
            cabecasDeChave[indexSlot1] = rank1;

            //Rank 2 vai para baixo (6 ou 8)
            int indexSlot2 = slotsParesInferior.get(random.nextInt(slotsParesInferior.size())) - 1;
            cabecasDeChave[indexSlot2] = rank2;
        } else {
            //Inverte: Rank 1 vai para baixo (6 ou 8)
            int indexSlot1 = slotsParesInferior.get(random.nextInt(slotsParesInferior.size())) - 1;
            cabecasDeChave[indexSlot1] = rank1;

            //Rank 2 vai para cima (2 ou 4)
            int indexSlot2 = slotsParesSuperior.get(random.nextInt(slotsParesSuperior.size())) - 1;
            cabecasDeChave[indexSlot2] = rank2;
        }

        //Preenche o restante do Pote A aleatoriamente nos slots vazios
        Collections.shuffle(poteA);
        Iterator<ParticipacaoFase> it = poteA.iterator();
        for (int i = 0; i < totalConfrontos; i++) {
            if (cabecasDeChave[i] == null) {
                cabecasDeChave[i] = it.next();
            }
        }

        //Geração das Partidas
        List<Partida> partidas = new ArrayList<>();
        for (int i = 0; i < totalConfrontos; i++) {
            int chaveIndex = i + 1; //visual: 1, 2, 3...
            ParticipacaoFase favorito = cabecasDeChave[i];
            ParticipacaoFase desafiante = poteB.get(i);

            partidas.addAll(criarConfronto(fase, chaveIndex, favorito, desafiante));
        }

        return partidas;
    }

    //Retorna lista de números PARES dentro do intervalo
    private List<Integer> gerarSlotsPares(int inicio, int fim) {
        List<Integer> pares = new ArrayList<>();
        for (int i = inicio; i <= fim; i++) {
            if (i % 2 == 0) pares.add(i);
        }
        //Fallback: Se não houver par (ex: final só tem jogo 1), retorna o próprio intervalo
        if (pares.isEmpty()) {
            for (int i = inicio; i <= fim; i++) pares.add(i);
        }
        return pares;
    }

    private List<Partida> criarConfronto(FaseTorneio fase, int index, ParticipacaoFase fav, ParticipacaoFase des) {
        List<Partida> lista = new ArrayList<>();

        FaseMataMata etapa = fase.getFaseInicialMataMata();
        boolean isFinal = etapa == FaseMataMata.FINAL;

        if (Boolean.TRUE.equals(fase.getTemJogoVolta())) {
            //Ida
            Partida p1 = criarBase(fase, index);
            p1.setMandante(des.getJogadorClube());
            p1.setVisitante(fav.getJogadorClube());
            p1.setLogEventos("Ida");
            p1.setTipoPartida(isFinal ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA);
            lista.add(p1);
            //Volta
            Partida p2 = criarBase(fase, index);
            p2.setMandante(fav.getJogadorClube());
            p2.setVisitante(des.getJogadorClube());
            p2.setLogEventos("Volta");
            p2.setTipoPartida(isFinal ? TipoPartida.FINAL_VOLTA : TipoPartida.MATA_MATA_VOLTA);
            lista.add(p2);
        } else {
            Partida p = criarBase(fase, index);
            p.setMandante(fav.getJogadorClube());
            p.setVisitante(des.getJogadorClube());
            p.setLogEventos("Único");
            p.setTipoPartida(isFinal ? TipoPartida.FINAL_UNICA : TipoPartida.MATA_MATA_UNICO);
            lista.add(p);
        }
        return lista;
    }

    private Partida criarBase(FaseTorneio f, int i) {
        Partida p = new Partida();
        p.setFase(f);
        p.setEtapaMataMata(f.getFaseInicialMataMata());
        p.setChaveIndex(i);
        p.setRealizada(false);
        return p;
    }

    private void validarQuantidadeVsEtapa(int n, FaseMataMata etapa) {
        int esperado = etapa.getNumeroTimes();

        if (n != esperado) {
            throw new IllegalArgumentException(
                    String.format("Erro de consistência: A fase %s exige exatos %d jogadores, mas foram enviados %d.",
                            etapa, esperado, n)
            );
        }
    }
}