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

        if (usarAlgoritmoPadrao) {
            return gerarAlgoritmoPadrao(fase, participantes, rodadasDesejadas, n);
        } else {
            return gerarAlgoritmoCaos(fase, participantes, rodadasDesejadas, n);
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

        List<Rodada> todasRodadas = new ArrayList<>();

        //embaralha a lista inicial para mudar o pivô
        List<ParticipacaoFase> listaRotativa = new ArrayList<>(participantes);
        Collections.shuffle(listaRotativa);

        Map<String, Integer> contagemJogosEmCasa = new HashMap<>();
        for (ParticipacaoFase p : participantes) {
            contagemJogosEmCasa.put(p.getId(), 0);
        }

        //fator de aleatoriedade para inversão do critério de desempate
        boolean inverteMandoBase = random.nextBoolean();

        //loop de geração (Round Robin)
        for (int r = 0; r < rodadasDesejadas; r++) {

            Rodada rodada = new Rodada();
            rodada.setFase(fase);
            rodada.setStatus(StatusRodada.ABERTA);

            List<Partida> partidasDestaRodada = new ArrayList<>();
            int numJogosNaRodada = n / 2;

            for (int i = 0; i < numJogosNaRodada; i++) {
                ParticipacaoFase p1 = listaRotativa.get(i);
                ParticipacaoFase p2 = listaRotativa.get(n - 1 - i);

                String id1 = p1.getId();
                String id2 = p2.getId();
                int casa1 = contagemJogosEmCasa.get(id1);
                int casa2 = contagemJogosEmCasa.get(id2);

                boolean p1Manda;

                //prioridade 1: balanceamento absoluto
                if (casa1 < casa2) {
                    p1Manda = true;
                } else if (casa2 < casa1) {
                    p1Manda = false;
                } else {
                    //prioridade 2: quebra de padrão algorítmico
                    if (i == 0) {
                        //o pivô alterna mando a cada rodada
                        p1Manda = (r % 2 == 0) ^ inverteMandoBase;
                    } else {
                        //outros pares alternam com base na rodada e posição
                        p1Manda = ((r + i) % 2 != 0) ^ inverteMandoBase;
                    }
                }

                JogadorClube mandante;
                JogadorClube visitante;

                if (p1Manda) {
                    mandante = p1.getJogadorClube();
                    visitante = p2.getJogadorClube();
                    contagemJogosEmCasa.put(id1, casa1 + 1);
                } else {
                    mandante = p2.getJogadorClube();
                    visitante = p1.getJogadorClube();
                    contagemJogosEmCasa.put(id2, casa2 + 1);
                }

                Partida partida = new Partida();
                partida.setFase(fase);
                partida.setRodada(rodada);
                partida.setMandante(mandante);
                partida.setVisitante(visitante);
                partida.setRealizada(false);

                partidasDestaRodada.add(partida);
            }

            //embaralha a ordem dos jogos dentro da rodada
            Collections.shuffle(partidasDestaRodada);

            rodada.setPartidas(partidasDestaRodada);
            todasRodadas.add(rodada);

            //rotação do Círculo
            ParticipacaoFase ultimo = listaRotativa.remove(listaRotativa.size() - 1);
            listaRotativa.add(1, ultimo);
        }

        //embaralha a ordem das rodadas
        //isso impede que a sequência de adversários seja previsível
        Collections.shuffle(todasRodadas);

        //reatribui os números das rodadas sequencialmente após o embaralhamento
        for (int i = 0; i < todasRodadas.size(); i++) {
            todasRodadas.get(i).setNumero(i + 1);
        }

        return todasRodadas;
    }
}