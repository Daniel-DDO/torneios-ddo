package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorLigaBalanceadaStrategy implements GeradorPartidasStrategy<Rodada> {

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

        //Preparação dos índices para o círculo
        List<ParticipacaoFase> listaRotativa = new ArrayList<>(participantes);

        //Map para controlar quantos jogos em casa cada ID já fez
        Map<String, Integer> contagemJogosEmCasa = new HashMap<>();
        for (ParticipacaoFase p : participantes) {
            if (p.getId() == null) {
                throw new IllegalStateException("Os participantes precisam estar salvos no banco (ID não nulo) antes de gerar a tabela.");
            }
            contagemJogosEmCasa.put(p.getId(), 0);
        }

        List<Rodada> rodadasCriadas = new ArrayList<>();

        //Loop de geração das rodadas
        for (int r = 1; r <= rodadasDesejadas; r++) {

            Rodada rodada = new Rodada();
            rodada.setFase(fase);
            rodada.setNumero(r);
            rodada.setStatus(StatusRodada.ABERTA);

            List<Partida> partidasDestaRodada = new ArrayList<>();
            int numJogosNaRodada = n / 2;

            //Lógica do círculo
            for (int i = 0; i < numJogosNaRodada; i++) {
                //Pegamos os oponentes na "mesa"
                //O índice 0 é o pivô fixo. O oponente dele é o último da lista.
                ParticipacaoFase p1 = listaRotativa.get(i);
                ParticipacaoFase p2 = listaRotativa.get(n - 1 - i);

                //definição inteligente de mando de campo
                JogadorClube mandante;
                JogadorClube visitante;

                int casaP1 = contagemJogosEmCasa.get(p1.getId());
                int casaP2 = contagemJogosEmCasa.get(p2.getId());

                //regra 1: quem jogou menos em casa tem prioridade absoluta
                boolean p1Manda;

                if (casaP1 < casaP2) {
                    p1Manda = true;
                } else if (casaP2 < casaP1) {
                    p1Manda = false;
                } else {
                    //regra 2: empate no histórico -> alternância matemática para quebrar padrões
                    //a soma (rodada + indice) garante que o mando rotacione mesmo com histórico igual
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
                partida.setRodada(rodada); //vínculo bidirecional importante
                partida.setMandante(mandante);
                partida.setVisitante(visitante);

                partidasDestaRodada.add(partida);
            }

            rodada.setPartidas(partidasDestaRodada);
            rodadasCriadas.add(rodada);

            //rotação do círculo (Round Robin) ---
            //remove o último elemento e insere na posição 1.
            //o elemento da posição 0 (pivô) nunca se move.
            ParticipacaoFase ultimo = listaRotativa.remove(listaRotativa.size() - 1);
            listaRotativa.add(1, ultimo);
        }

        return rodadasCriadas;
    }
}