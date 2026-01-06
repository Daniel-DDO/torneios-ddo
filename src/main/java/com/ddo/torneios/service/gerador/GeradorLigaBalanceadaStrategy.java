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

        boolean comBacktracking = random.nextBoolean();

        if (comBacktracking) {
            Map<String, ControleTime> controle = new HashMap<>();
            for (ParticipacaoFase p : participantes) {
                controle.put(p.getId(), new ControleTime(p));
            }

            List<Rodada> rodadas = new ArrayList<>();
            for (int i = 1; i <= rodadasDesejadas; i++) {
                Rodada r = new Rodada();
                r.setFase(fase);
                r.setNumero(i);
                r.setStatus(StatusRodada.ABERTA);
                r.setPartidas(new ArrayList<>());
                rodadas.add(r);
            }

            int maxMandos = rodadasDesejadas / 2;

            if (gerarAlgoritmoBacktracking(0, rodadas, participantes, controle, maxMandos)) {
                return rodadas;
            }
        }

        return gerarAlgoritmoBalanceado(fase, participantes, rodadasDesejadas, n);
    }

    //ALGORITMO 1: sorteio inicial + matemática
    private List<Rodada> gerarAlgoritmoBalanceado(FaseTorneio fase, List<ParticipacaoFase> participantes, int rodadasDesejadas, int n) {

        List<ParticipacaoFase> listaRotativa = new ArrayList<>(participantes);

        Collections.shuffle(listaRotativa);

        boolean inverterMandoGlobal = random.nextBoolean();

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
                boolean p1Manda;

                if (i == 0) {
                    p1Manda = (r % 2 != 0);
                } else {
                    p1Manda = (i % 2 != 0);
                }

                if (inverterMandoGlobal) {
                    p1Manda = !p1Manda;
                }

                if (p1Manda) {
                    mandante = p1.getJogadorClube();
                    visitante = p2.getJogadorClube();
                } else {
                    mandante = p2.getJogadorClube();
                    visitante = p1.getJogadorClube();
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

            Collections.shuffle(partidasDestaRodada);

            rodada.setPartidas(partidasDestaRodada);
            rodadasCriadas.add(rodada);

            ParticipacaoFase ultimo = listaRotativa.remove(listaRotativa.size() - 1);
            listaRotativa.add(1, ultimo);
        }

        return rodadasCriadas;
    }

    //ALGORITMO 2: Com backtracking
    private boolean gerarAlgoritmoBacktracking(
            int rodadaIndex,
            List<Rodada> rodadas,
            List<ParticipacaoFase> todosParticipantes,
            Map<String, ControleTime> controle,
            int maxMandos
    ) {
        if (rodadaIndex == rodadas.size()) {
            return true;
        }

        Rodada rodadaAtual = rodadas.get(rodadaIndex);

        List<ParticipacaoFase> pendentesNestaRodada = new ArrayList<>(todosParticipantes);
        Collections.shuffle(pendentesNestaRodada);

        return backtrackPartidas(rodadaAtual, pendentesNestaRodada, controle, maxMandos, rodadaIndex, rodadas, todosParticipantes);
    }

    private boolean backtrackPartidas(
            Rodada rodadaAtual,
            List<ParticipacaoFase> pendentes,
            Map<String, ControleTime> controle,
            int maxMandos,
            int rodadaIndexGlobal,
            List<Rodada> todasRodadas,
            List<ParticipacaoFase> todosParticipantesOriginal
    ) {
        if (pendentes.isEmpty()) {
            return gerarAlgoritmoBacktracking(rodadaIndexGlobal + 1, todasRodadas, todosParticipantesOriginal, controle, maxMandos);
        }

        ParticipacaoFase mandante = pendentes.get(0);
        ControleTime ctrlMandante = controle.get(mandante.getId());

        if (ctrlMandante.qtdCasa >= maxMandos) {
            return false;
        }

        for (int i = 1; i < pendentes.size(); i++) {
            ParticipacaoFase visitante = pendentes.get(i);
            ControleTime ctrlVisitante = controle.get(visitante.getId());

            //REGRAS DE VALIDAÇÃO
            if (ctrlVisitante.qtdFora >= maxMandos) continue;

            if (ctrlMandante.adversariosEnfrentados.contains(visitante.getId())) continue;

            Partida partida = new Partida();
            partida.setFase(rodadaAtual.getFase());
            partida.setRodada(rodadaAtual);
            partida.setMandante(mandante.getJogadorClube());
            partida.setVisitante(visitante.getJogadorClube());
            partida.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);

            rodadaAtual.getPartidas().add(partida);
            ctrlMandante.registrarJogo(true, visitante.getId());
            ctrlVisitante.registrarJogo(false, mandante.getId());

            List<ParticipacaoFase> novosPendentes = new ArrayList<>(pendentes);
            novosPendentes.remove(mandante);
            novosPendentes.remove(visitante);

            if (backtrackPartidas(rodadaAtual, novosPendentes, controle, maxMandos, rodadaIndexGlobal, todasRodadas, todosParticipantesOriginal)) {
                return true;
            }

            //BACKTRACK (se falhou lá na frente, desfaz tudo e tenta o próximo 'i')
            rodadaAtual.getPartidas().remove(partida);
            ctrlMandante.desfazerJogo(true, visitante.getId());
            ctrlVisitante.desfazerJogo(false, mandante.getId());
        }

        return false;
    }

    private static class ControleTime {
        ParticipacaoFase participacao;
        int qtdCasa = 0;
        int qtdFora = 0;
        Set<String> adversariosEnfrentados = new HashSet<>();

        public ControleTime(ParticipacaoFase p) {
            this.participacao = p;
        }

        void registrarJogo(boolean ehMandante, String idAdversario) {
            if (ehMandante) qtdCasa++;
            else qtdFora++;
            adversariosEnfrentados.add(idAdversario);
        }

        void desfazerJogo(boolean ehMandante, String idAdversario) {
            if (ehMandante) qtdCasa--;
            else qtdFora--;
            adversariosEnfrentados.remove(idAdversario);
        }
    }
}