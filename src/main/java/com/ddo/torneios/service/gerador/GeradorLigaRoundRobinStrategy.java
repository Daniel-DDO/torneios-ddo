package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorLigaRoundRobinStrategy implements GeradorPartidasStrategy<Rodada> {

    @Override
    public List<Rodada> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();
        if (n % 2 != 0) {
            throw new IllegalArgumentException("O número de participantes deve ser PAR para a liga round-robin.");
        }

        List<ParticipacaoFase> listaRotativa = new ArrayList<>(participantes);
        Collections.shuffle(listaRotativa);

        int numRodadasIda = n - 1;
        List<Rodada> todasRodadas = new ArrayList<>();

        for (int r = 0; r < numRodadasIda; r++) {
            Rodada rodada = criarRodadaBase(fase, r + 1);
            List<Partida> partidas = new ArrayList<>();

            for (int i = 0; i < n / 2; i++) {
                ParticipacaoFase p1 = listaRotativa.get(i);
                ParticipacaoFase p2 = listaRotativa.get(n - 1 - i);

                boolean p1Manda = (r + i) % 2 == 0;

                partidas.add(criarPartidaLiga(fase, rodada,
                        p1Manda ? p1.getJogadorClube() : p2.getJogadorClube(),
                        p1Manda ? p2.getJogadorClube() : p1.getJogadorClube()));
            }
            rodada.setPartidas(partidas);
            todasRodadas.add(rodada);

            ParticipacaoFase ultimo = listaRotativa.remove(n - 1);
            listaRotativa.add(1, ultimo);
        }

        if (Boolean.TRUE.equals(fase.getTemJogoVolta())) {
            List<Rodada> returno = new ArrayList<>();
            for (Rodada ida : todasRodadas) {
                Rodada rodadaVolta = criarRodadaBase(fase, ida.getNumero() + numRodadasIda);
                List<Partida> partidasVolta = new ArrayList<>();

                for (Partida pIda : ida.getPartidas()) {
                    partidasVolta.add(criarPartidaLiga(fase, rodadaVolta, pIda.getVisitante(), pIda.getMandante()));
                }
                rodadaVolta.setPartidas(partidasVolta);
                returno.add(rodadaVolta);
            }
            todasRodadas.addAll(returno);
        }

        return todasRodadas;
    }

    private Rodada criarRodadaBase(FaseTorneio f, int num) {
        Rodada r = new Rodada();
        r.setFase(f);
        r.setNumero(num);
        r.setStatus(StatusRodada.ABERTA);
        return r;
    }

    private Partida criarPartidaLiga(FaseTorneio f, Rodada r, JogadorClube m, JogadorClube v) {
        Partida p = new Partida();
        p.setFase(f);
        p.setRodada(r);
        p.setMandante(m);
        p.setVisitante(v);
        p.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);
        p.setRealizada(false);
        return p;
    }
}