package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeradorMataMataPotesManuaisStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        List<ParticipacaoFase> pote1 = participantes.stream()
                .filter(p -> "Pote 1".equalsIgnoreCase(p.getGrupo())).collect(Collectors.toList());
        List<ParticipacaoFase> pote2 = participantes.stream()
                .filter(p -> "Pote 2".equalsIgnoreCase(p.getGrupo())).collect(Collectors.toList());

        if (pote1.size() != pote2.size()) {
            throw new IllegalArgumentException("Os potes devem ter o mesmo tamanho para o sorteio.");
        }

        Collections.shuffle(pote1);
        Collections.shuffle(pote2);

        List<Partida> partidasIniciais = new ArrayList<>();
        for (int i = 0; i < pote1.size(); i++) {
            partidasIniciais.addAll(criarConfronto(fase, i + 1, pote1.get(i), pote2.get(i), "Potes Manuais"));
        }

        vincularProximasFases(partidasIniciais, fase);

        return partidasIniciais;
    }
}