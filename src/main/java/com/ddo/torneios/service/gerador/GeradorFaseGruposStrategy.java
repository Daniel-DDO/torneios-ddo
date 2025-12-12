package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorFaseGruposStrategy implements GeradorPartidasStrategy<Partida> {

    private static final int TAMANHO_PADRAO_GRUPO = 4;

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int nTotal = participantes.size();

        int qtdGrupos = nTotal / TAMANHO_PADRAO_GRUPO;

        if (qtdGrupos == 0 && nTotal > 0 && nTotal % 2 == 0) {
            qtdGrupos = 1;
        } else if (qtdGrupos == 0) {
            throw new IllegalArgumentException("Número insuficiente de participantes para formar grupos.");
        }

        int tamanhoRealGrupo = nTotal / qtdGrupos;

        if (nTotal % qtdGrupos != 0) {
            throw new IllegalArgumentException(String.format(
                    "Impossível dividir %d jogadores em %d grupos exatos. Sobrariam jogadores.",
                    nTotal, qtdGrupos));
        }

        if (tamanhoRealGrupo % 2 != 0) {
            throw new IllegalArgumentException(String.format(
                    "A divisão resultou em grupos de %d jogadores. " +
                            "A regra exige que a quantidade por grupo seja PAR para o algoritmo de confronto.",
                    tamanhoRealGrupo));
        }

        List<Partida> todasPartidas = new ArrayList<>();

        List<ParticipacaoFase> sorteio = new ArrayList<>(participantes);
        Collections.shuffle(sorteio);

        for (int i = 0; i < qtdGrupos; i++) {
            String nomeGrupo = getNomeGrupo(i);

            int inicio = i * tamanhoRealGrupo;
            int fim = inicio + tamanhoRealGrupo;

            List<ParticipacaoFase> grupoAtual = sorteio.subList(inicio, fim);

            for (ParticipacaoFase p : grupoAtual) {
                p.setGrupo(nomeGrupo);
            }

            todasPartidas.addAll(gerarBergerPorGrupo(fase, grupoAtual, nomeGrupo));
        }

        return todasPartidas;
    }

    private List<Partida> gerarBergerPorGrupo(FaseTorneio fase, List<ParticipacaoFase> jogadores, String nomeGrupo) {
        List<Partida> partidasDoGrupo = new ArrayList<>();
        int n = jogadores.size();

        int rodadasIda = n - 1;
        int jogosPorRodada = n / 2;

        List<ParticipacaoFase> roleta = new ArrayList<>(jogadores);

        for (int r = 0; r < rodadasIda; r++) {
            int numRodadaIda = r + 1;
            int numRodadaVolta = numRodadaIda + rodadasIda;

            for (int i = 0; i < jogosPorRodada; i++) {
                int index1 = i;
                int index2 = n - 1 - i;

                ParticipacaoFase p1 = roleta.get(index1);
                ParticipacaoFase p2 = roleta.get(index2);

                boolean p1Mandante = (r % 2 == 0);

                if (i == 0) {
                    p1Mandante = (r % 2 == 0);
                } else {
                    p1Mandante = (r % 2 != 0);
                }
                if (n == 4) p1Mandante = (r % 2 == 0);

                Partida ida = criarPartidaBase(fase, nomeGrupo);
                ida.setMandante(p1Mandante ? p1.getJogadorClube() : p2.getJogadorClube());
                ida.setVisitante(p1Mandante ? p2.getJogadorClube() : p1.getJogadorClube());
                ida.setLogEventos("Grupo " + nomeGrupo + " - Rodada " + numRodadaIda);
                partidasDoGrupo.add(ida);

                if (Boolean.TRUE.equals(fase.getTemJogoVolta())) {
                    Partida volta = criarPartidaBase(fase, nomeGrupo);
                    volta.setMandante(p1Mandante ? p2.getJogadorClube() : p1.getJogadorClube());
                    volta.setVisitante(p1Mandante ? p1.getJogadorClube() : p2.getJogadorClube());
                    volta.setLogEventos("Grupo " + nomeGrupo + " - Rodada " + numRodadaVolta);
                    partidasDoGrupo.add(volta);
                }
            }

            ParticipacaoFase ultimo = roleta.remove(roleta.size() - 1);
            roleta.add(1, ultimo);
        }

        return partidasDoGrupo;
    }

    private Partida criarPartidaBase(FaseTorneio f, String grupo) {
        Partida p = new Partida();
        p.setFase(f);
        p.setRealizada(false);
        p.setWo(false);
        p.setTipoPartida(TipoPartida.FASE_DE_GRUPOS);
        return p;
    }

    private String getNomeGrupo(int index) {
        return String.valueOf((char) ('A' + index));
    }
}