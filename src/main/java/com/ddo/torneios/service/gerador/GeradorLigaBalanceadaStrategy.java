package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorLigaBalanceadaStrategy implements GeradorPartidasStrategy<Rodada> {

    private final Random random = new Random();

    @Override
    public List<Rodada> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {

        int n = participantes.size();
        Integer rodadasDesejadas = fase.getNumeroRodadas();

        if (rodadasDesejadas == null) {
            throw new IllegalArgumentException("O número de rodadas não foi definido na Fase.");
        }

        if (n % 2 != 0) {
            throw new IllegalArgumentException("O número de participantes deve ser PAR. (Atual: " + n + "). Adicione um 'Bye' ou remova um jogador.");
        }
        if (rodadasDesejadas > n - 1) {
            throw new IllegalArgumentException("O número de rodadas (" + rodadasDesejadas + ") não pode ser maior que o número de oponentes possíveis (" + (n - 1) + ").");
        }
        if (rodadasDesejadas % 2 != 0) {
            throw new IllegalArgumentException("Para garantir balanceamento perfeito de casa/fora, o número de rodadas deve ser PAR.");
        }

        for (ParticipacaoFase p : participantes) {
            if (p.getId() == null) {
                throw new IllegalStateException("Os participantes precisam estar salvos no banco (ID não nulo) antes de gerar a tabela.");
            }
        }

        //se true, usa o algoritmo original (padrão). Se false, usa o de caos.
        boolean usarAlgoritmoPadrao = random.nextBoolean();

        //gambiarra temporaria
        if (usarAlgoritmoPadrao) {
            return gerarAlgoritmoPadrao(fase, participantes, rodadasDesejadas, n);
        } else {
            return gerarAlgoritmoPadrao(fase, participantes, rodadasDesejadas, n);
            //return gerarAlgoritmoCaos(fase, participantes, rodadasDesejadas, n);
        }
    }

    //ALGORITMO 1: PADRÃO (Estático, ordenado, sem embaralhamento prévio)
    private List<Rodada> gerarAlgoritmoPadrao(FaseTorneio fase, List<ParticipacaoFase> participantes, int rodadasDesejadas, int n) {

        List<ParticipacaoFase> listaRotativa = new ArrayList<>(participantes);

        //map para controlar quantos jogos em casa cada ID já fez
        Map<String, Integer> contagemJogosEmCasa = new HashMap<>();
        for (ParticipacaoFase p : participantes) {
            contagemJogosEmCasa.put(p.getId(), 0);
        }

        List<Rodada> rodadasCriadas = new ArrayList<>();

        for (int r = 1; r <= rodadasDesejadas; r++) {

            Rodada rodada = new Rodada();
            rodada.setFase(fase);
            rodada.setNumero(r);
            rodada.setStatus(StatusRodada.ABERTA);

            List<Partida> partidasDestaRodada = new ArrayList<>();
            int numJogosNaRodada = n / 2;

            for (int i = 0; i < numJogosNaRodada; i++) {
                ParticipacaoFase p1 = listaRotativa.get(i);
                ParticipacaoFase p2 = listaRotativa.get(n - 1 - i);

                JogadorClube mandante;
                JogadorClube visitante;

                int casaP1 = contagemJogosEmCasa.get(p1.getId());
                int casaP2 = contagemJogosEmCasa.get(p2.getId());

                boolean p1Manda;

                if (casaP1 < casaP2) {
                    p1Manda = true;
                } else if (casaP2 < casaP1) {
                    p1Manda = false;
                } else {
                    //empate no histórico -> alternância matemática padrão
                    p1Manda = (r + i) % 2 != 0;
                }

                if (p1Manda) {
                    mandante = p1.getJogadorClube();
                    visitante = p2.getJogadorClube();
                    contagemJogosEmCasa.put(p1.getId(), casaP1 + 1);
                } else {
                    mandante = p2.getJogadorClube();
                    visitante = p1.getJogadorClube();
                    contagemJogosEmCasa.put(p2.getId(), casaP2 + 1);
                }

                Partida partida = new Partida();
                partida.setFase(fase);
                partida.setRodada(rodada);
                partida.setMandante(mandante);
                partida.setVisitante(visitante);
                partida.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);
                partida.setRealizada(false);

                partidasDestaRodada.add(partida);
            }

            rodada.setPartidas(partidasDestaRodada);
            rodadasCriadas.add(rodada);

            //rotação padrão
            ParticipacaoFase ultimo = listaRotativa.remove(listaRotativa.size() - 1);
            listaRotativa.add(1, ultimo);
        }

        return rodadasCriadas;
    }

    //ALGORITMO 2: CAOS BALANCEADO (Embaralha times, rodadas e inverte lógica de mando)
    private List<Rodada> gerarAlgoritmoCaos(FaseTorneio fase, List<ParticipacaoFase> participantes, int rodadasDesejadas, int n) {
        List<ParticipacaoFase> listaEmbaralhada = new ArrayList<>(participantes);

        Collections.shuffle(listaEmbaralhada);
        boolean inverterTudo = random.nextBoolean();

        return gerarCore(fase, listaEmbaralhada, rodadasDesejadas, n, inverterTudo);
    }

    private List<Rodada> gerarCore(FaseTorneio fase, List<ParticipacaoFase> listaRotativa, int rodadasDesejadas, int n, boolean inverterMandosGlobal) {

        List<Rodada> todasRodadas = new ArrayList<>();

        for (int r = 0; r < rodadasDesejadas; r++) {

            Rodada rodada = new Rodada();
            rodada.setFase(fase);
            rodada.setNumero(r + 1);
            rodada.setStatus(StatusRodada.ABERTA);

            List<Partida> partidasDestaRodada = new ArrayList<>();
            int numJogosNaRodada = n / 2;

            for (int i = 0; i < numJogosNaRodada; i++) {
                ParticipacaoFase p1 = listaRotativa.get(i);
                ParticipacaoFase p2 = listaRotativa.get(n - 1 - i);

                boolean p1Manda;

                if (i == 0) {
                    p1Manda = (r % 2 == 0);
                } else {
                    p1Manda = ((r + i) % 2 != 0);
                }

                if (inverterMandosGlobal) {
                    p1Manda = !p1Manda;
                }

                JogadorClube mandante = p1Manda ? p1.getJogadorClube() : p2.getJogadorClube();
                JogadorClube visitante = p1Manda ? p2.getJogadorClube() : p1.getJogadorClube();

                Partida partida = new Partida();
                partida.setFase(fase);
                partida.setRodada(rodada);
                partida.setMandante(mandante);
                partida.setVisitante(visitante);
                partida.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);
                partida.setRealizada(false);

                partidasDestaRodada.add(partida);
            }

            Collections.shuffle(partidasDestaRodada);

            rodada.setPartidas(partidasDestaRodada);
            todasRodadas.add(rodada);

            ParticipacaoFase ultimo = listaRotativa.remove(listaRotativa.size() - 1);
            listaRotativa.add(1, ultimo);
        }

        return todasRodadas;
    }
}