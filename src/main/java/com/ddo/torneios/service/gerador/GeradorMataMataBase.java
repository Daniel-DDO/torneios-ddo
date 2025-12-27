package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import java.util.*;

public abstract class GeradorMataMataBase {

    protected List<Partida> criarConfronto(FaseTorneio fase, int index, ParticipacaoFase p1, ParticipacaoFase p2, String prefixoLog) {
        List<Partida> lista = new ArrayList<>();
        FaseMataMata etapa = fase.getFaseInicialMataMata();
        boolean ehFinal = (etapa == FaseMataMata.FINAL);
        boolean temVolta = ehFinal ? !Boolean.TRUE.equals(fase.getFinalJogoUnico()) : Boolean.TRUE.equals(fase.getTemJogoVolta());

        if (temVolta) {
            lista.add(montarPartida(fase, etapa, index, p2, p1, prefixoLog + " - Ida",
                    ehFinal ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA));
            lista.add(montarPartida(fase, etapa, index, p1, p2, prefixoLog + " - Volta",
                    ehFinal ? TipoPartida.FINAL_VOLTA : TipoPartida.MATA_MATA_VOLTA));
        } else {
            lista.add(montarPartida(fase, etapa, index, p1, p2, prefixoLog + " - Único",
                    ehFinal ? TipoPartida.FINAL_UNICA : TipoPartida.MATA_MATA_UNICO));
        }
        return lista;
    }

    protected Partida montarPartida(FaseTorneio f, FaseMataMata e, int i, ParticipacaoFase mandante, ParticipacaoFase visitante, String log, TipoPartida t) {
        Partida p = new Partida();
        p.setFase(f);
        p.setEtapaMataMata(e);
        p.setChaveIndex(i);
        p.setMandante(mandante != null ? mandante.getJogadorClube() : null);
        p.setVisitante(visitante != null ? visitante.getJogadorClube() : null);
        p.setLogEventos(log);
        p.setTipoPartida(t);
        p.setRealizada(false);
        return p;
    }

    protected void vincularProximasFases(List<Partida> partidasIniciais, FaseTorneio fase) {
        FaseMataMata etapaAtual = fase.getFaseInicialMataMata();
        List<FaseMataMata> caminho = obterCaminhoAteFinal(etapaAtual);

        List<Partida> anteriores = partidasIniciais;
        for (FaseMataMata proxima : caminho) {
            List<Partida> novas = new ArrayList<>();
            int totalConfrontos = proxima.getNumeroTimes() / 2;

            for (int i = 1; i <= totalConfrontos; i++) {
                List<Partida> confrontoVazio = criarConfrontoVazio(fase, proxima, i);
                novas.addAll(confrontoVazio);
                conectarVencedores(anteriores, confrontoVazio.get(0), i);
            }
            partidasIniciais.addAll(novas);
            anteriores = novas;
        }
    }

    private List<Partida> criarConfrontoVazio(FaseTorneio f, FaseMataMata e, int idx) {
        boolean ehFinal = (e == FaseMataMata.FINAL);
        boolean temVolta = ehFinal ? !Boolean.TRUE.equals(f.getFinalJogoUnico()) : Boolean.TRUE.equals(f.getTemJogoVolta());
        List<Partida> lista = new ArrayList<>();
        if (temVolta) {
            lista.add(montarPartida(f, e, idx, null, null, e.name() + " - Ida", ehFinal ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA));
            lista.add(montarPartida(f, e, idx, null, null, e.name() + " - Volta", ehFinal ? TipoPartida.FINAL_VOLTA : TipoPartida.MATA_MATA_VOLTA));
        } else {
            lista.add(montarPartida(f, e, idx, null, null, e.name() + " - Único", ehFinal ? TipoPartida.FINAL_UNICA : TipoPartida.MATA_MATA_UNICO));
        }
        return lista;
    }

    private void conectarVencedores(List<Partida> anteriores, Partida proximaMestra, int indexProxima) {
        for (Partida p : anteriores) {
            if ((p.getChaveIndex() + 1) / 2 == indexProxima) {
                p.setProximaPartida(proximaMestra);
                p.setSlotNaProxima(p.getChaveIndex() % 2 != 0 ? 1 : 2);
            }
        }
    }

    private List<FaseMataMata> obterCaminhoAteFinal(FaseMataMata inicial) {
        List<FaseMataMata> lista = new ArrayList<>();
        FaseMataMata[] todos = FaseMataMata.values();

        int idxInicial = -1;
        for(int i=0; i<todos.length; i++) if(todos[i] == inicial) idxInicial = i;

        for (int i = idxInicial - 1; i >= 0; i--) {
            lista.add(todos[i]);
        }
        return lista;
    }

    protected void validarQuantidadeParticipantes(int n, FaseMataMata etapa) {
        int esperado = etapa.getNumeroTimes();
        if (n != esperado) {
            throw new IllegalArgumentException(
                    String.format("Erro de consistência: A fase %s exige exatos %d jogadores, mas foram enviados %d.",
                            etapa.name(), esperado, n)
            );
        }
    }
}