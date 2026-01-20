package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorMataMataSorteioTotalStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();

        if (n < 2 || n % 2 != 0) {
            throw new IllegalArgumentException("Número de participantes deve ser PAR e maior que 1 para o sorteio total.");
        }

        validarQuantidadeParticipantes(n, fase.getFaseInicialMataMata());

        List<ParticipacaoFase> sorteio = new ArrayList<>(participantes);

        Collections.shuffle(sorteio);
        Collections.shuffle(sorteio);

        List<Partida> partidasGeradas = new ArrayList<>();
        FaseMataMata faseInicial = fase.getFaseInicialMataMata();
        String nomeFaseLog = faseInicial.name();
        int totalConfrontos = n / 2;

        for (int i = 0; i < totalConfrontos; i++) {
            ParticipacaoFase p1 = sorteio.get(i * 2);
            ParticipacaoFase p2 = sorteio.get(i * 2 + 1);

            List<Partida> confronto = criarConfronto(
                    fase,
                    i + 1,
                    p1,
                    p2,
                    nomeFaseLog
            );

            confronto.forEach(p -> p.setEtapaMataMata(faseInicial));

            partidasGeradas.addAll(confronto);
        }

        vincularProximasFases(partidasGeradas, fase);

        return partidasGeradas;
    }
}