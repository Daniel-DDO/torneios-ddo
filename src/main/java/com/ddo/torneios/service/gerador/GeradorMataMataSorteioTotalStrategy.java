package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorMataMataSorteioTotalStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        if (participantes.size() % 2 != 0) {
            throw new IllegalArgumentException("Número de participantes deve ser par para o sorteio.");
        }

        List<ParticipacaoFase> sorteio = new ArrayList<>(participantes);
        Collections.shuffle(sorteio);

        List<Partida> partidas = new ArrayList<>();
        int chave = 1;

        while (!sorteio.isEmpty()) {
            ParticipacaoFase p1 = sorteio.remove(0);
            ParticipacaoFase p2 = sorteio.remove(0);

            partidas.addAll(criarConfronto(fase, chave++, p1, p2, "Sorteio Total"));
        }

        return partidas;
    }
}