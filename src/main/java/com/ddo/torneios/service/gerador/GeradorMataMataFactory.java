package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.Partida; // Import necessário
import com.ddo.torneios.model.TipoGeracao;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;

@Component
public class GeradorMataMataFactory {

    private final Map<TipoGeracao, GeradorPartidasStrategy<Partida>> estrategias = new EnumMap<>(TipoGeracao.class);

    public GeradorMataMataFactory(List<GeradorPartidasStrategy<Partida>> listaEstrategias) {
        for (GeradorPartidasStrategy<Partida> strategy : listaEstrategias) {
            if (strategy instanceof GeradorMataMataSorteioDirigidoStrategy) {
                estrategias.put(TipoGeracao.SORTEIO_DIRIGIDO, strategy);
            } else if (strategy instanceof GeradorMataMataRankingStrategy) {
                estrategias.put(TipoGeracao.RANKING_PURO, strategy);
            } else if (strategy instanceof GeradorMataMataSorteioTotalStrategy) {
                estrategias.put(TipoGeracao.SORTEIO_TOTAL, strategy);
            } else if (strategy instanceof GeradorMataMataPotesManuaisStrategy) {
                estrategias.put(TipoGeracao.POTES_MANUAIS, strategy);
            }
        }
    }

    public GeradorPartidasStrategy<Partida> obterEstrategia(TipoGeracao tipo) {
        GeradorPartidasStrategy<Partida> strategy = estrategias.get(tipo);
        if (strategy == null) {
            throw new IllegalArgumentException("Nenhuma estratégia encontrada para o tipo: " + tipo);
        }
        return strategy;
    }
}