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

        List<Partida> todasPartidas = new ArrayList<>();
        List<Partida> rodadaInicialMestres = new ArrayList<>();

        for (int i = 0; i < totalConfrontos; i++) {
            List<Partida> confronto = criarConfronto(fase, i + 1, cabecasDeChave[i], poteB.get(i), "Sorteio Dirigido");
            todasPartidas.addAll(confronto);
            rodadaInicialMestres.add(confronto.get(confronto.size() - 1));
        }

        construirArvoreAteFinal(fase, rodadaInicialMestres, todasPartidas);

        return todasPartidas;
    }

    private void construirArvoreAteFinal(FaseTorneio fase, List<Partida> rodadaAnterior, List<Partida> listaGlobal) {
        List<Partida> currentRound = rodadaAnterior;

        while (currentRound.size() > 1) {
            currentRound.sort(Comparator.comparingInt(Partida::getChaveIndex));

            List<Partida> nextRound = new ArrayList<>();
            FaseMataMata etapaAtual = currentRound.get(0).getEtapaMataMata();
            FaseMataMata proximaEtapa = obterProximaEtapa(etapaAtual);

            int novaChaveIndex = 1;

            for (int i = 0; i < currentRound.size(); i += 2) {
                Partida jogoOrigemA = currentRound.get(i);
                Partida jogoOrigemB = currentRound.get(i + 1);

                boolean isFinal = (proximaEtapa == FaseMataMata.FINAL);
                boolean temVolta = fase.getTemJogoVolta() && !isFinal;

                List<Partida> novoConfronto = criarConfrontoVazio(fase, proximaEtapa, novaChaveIndex, temVolta);
                listaGlobal.addAll(novoConfronto);

                Partida proximoJogoDecisivo = novoConfronto.get(novoConfronto.size() - 1);

                jogoOrigemA.setProximaPartida(proximoJogoDecisivo);
                jogoOrigemA.setSlotNaProxima(1);

                jogoOrigemB.setProximaPartida(proximoJogoDecisivo);
                jogoOrigemB.setSlotNaProxima(2);

                nextRound.add(proximoJogoDecisivo);
                novaChaveIndex++;
            }
            currentRound = nextRound;
        }
    }

    private List<Partida> criarConfrontoVazio(FaseTorneio fase, FaseMataMata etapa, int chave, boolean temVolta) {
        List<Partida> jogos = new ArrayList<>();

        if (temVolta) {
            Partida ida = new Partida();
            ida.setFase(fase);
            ida.setEtapaMataMata(etapa);
            ida.setChaveIndex(chave);
            ida.setTipoPartida(etapa == FaseMataMata.FINAL ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA);
            jogos.add(ida);

            Partida volta = new Partida();
            volta.setFase(fase);
            volta.setEtapaMataMata(etapa);
            volta.setChaveIndex(chave);
            volta.setTipoPartida(etapa == FaseMataMata.FINAL ? TipoPartida.FINAL_VOLTA : TipoPartida.MATA_MATA_VOLTA);
            jogos.add(volta);
        } else {
            Partida unico = new Partida();
            unico.setFase(fase);
            unico.setEtapaMataMata(etapa);
            unico.setChaveIndex(chave);
            unico.setTipoPartida(etapa == FaseMataMata.FINAL ? TipoPartida.FINAL_UNICA : TipoPartida.MATA_MATA_UNICO);
            jogos.add(unico);
        }
        return jogos;
    }

    private FaseMataMata obterProximaEtapa(FaseMataMata atual) {
        switch (atual) {
            case DEZESSEIS_AVOS: return FaseMataMata.OITAVAS;
            case OITAVAS: return FaseMataMata.QUARTAS;
            case QUARTAS: return FaseMataMata.SEMIFINAL;
            case SEMIFINAL: return FaseMataMata.FINAL;
            default: throw new IllegalStateException("Etapa inválida para progressão: " + atual);
        }
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