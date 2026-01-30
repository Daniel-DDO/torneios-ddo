package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.AlgoritmoGeracaoMataMata;
import com.ddo.torneios.model.Partida;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class GeradorMataMataFactory {

    private final Map<AlgoritmoGeracaoMataMata, GeradorPartidasStrategy<Partida>> estrategias = new EnumMap<>(AlgoritmoGeracaoMataMata.class);

    public GeradorMataMataFactory(List<GeradorPartidasStrategy<Partida>> listaEstrategias) {
        for (GeradorPartidasStrategy<Partida> strategy : listaEstrategias) {
            if (strategy instanceof GeradorMataMataSorteioDirigidoStrategy) {
                estrategias.put(AlgoritmoGeracaoMataMata.SORTEIO_DIRIGIDO, strategy);
            } else if (strategy instanceof GeradorMataMataRankingStrategy) {
                estrategias.put(AlgoritmoGeracaoMataMata.RANKING_PADRAO, strategy);
            } else if (strategy instanceof GeradorMataMataSorteioTotalStrategy) {
                estrategias.put(AlgoritmoGeracaoMataMata.SORTEIO_TOTAL, strategy);
            } else if (strategy instanceof GeradorMataMataPotesManuaisStrategy) {
                estrategias.put(AlgoritmoGeracaoMataMata.POTES_MANUAIS, strategy);
            } else if (strategy instanceof GeradorCopaLigaStrategy) {
                estrategias.put(AlgoritmoGeracaoMataMata.COPA_LIGA, strategy);
            }
        }
    }

    public GeradorPartidasStrategy<Partida> obterEstrategia(AlgoritmoGeracaoMataMata algoritmo) {
        GeradorPartidasStrategy<Partida> strategy = estrategias.get(algoritmo);
        if (strategy == null) {
            throw new IllegalArgumentException("Nenhuma estratégia de geração implementada para o algoritmo: " + algoritmo);
        }
        return strategy;
    }
}