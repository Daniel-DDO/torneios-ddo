package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.AlgoritmoGeracaoLiga;
import com.ddo.torneios.model.AlgoritmoGeracaoMataMata;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.TipoTorneio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeradorStrategyFactory {

    @Autowired
    private GeradorLigaRoundRobinStrategy roundRobinStrategy;
    @Autowired
    private GeradorLigaBalanceadaStrategy balanceadaStrategy;
    @Autowired
    private GeradorLigaSuicaStrategy suicaStrategy;
    @Autowired
    private GeradorFaseGruposStrategy gruposStrategy;

    @Autowired
    private GeradorMataMataRankingStrategy rankingStrategy;
    @Autowired
    private GeradorMataMataSorteioTotalStrategy sorteioTotalStrategy;
    @Autowired
    private GeradorMataMataSorteioDirigidoStrategy sorteioDirigidoStrategy;
    @Autowired
    private GeradorMataMataPotesManuaisStrategy potesManuaisStrategy;

    public GeradorPartidasStrategy<?> getStrategy(FaseTorneio fase) {
        if (fase.getTipoTorneio() == TipoTorneio.PONTOS_CORRIDOS) {
            return getLigaStrategy(fase.getAlgoritmoLiga());
        } else {
            return getMataMataStrategy(fase.getAlgoritmoMataMata());
        }
    }

    private GeradorPartidasStrategy<?> getLigaStrategy(AlgoritmoGeracaoLiga algoritmo) {
        return switch (algoritmo) {
            case TODOS_CONTRA_TODOS_IDA_VOLTA, TODOS_CONTRA_TODOS_UNICO -> roundRobinStrategy;
            case ALEATORIO_BALANCEADO -> balanceadaStrategy;
            case SISTEMA_SUICO -> suicaStrategy;
            case FASE_GRUPOS -> gruposStrategy;
        };
    }

    private GeradorPartidasStrategy<?> getMataMataStrategy(AlgoritmoGeracaoMataMata algoritmo) {
        return switch (algoritmo) {
            case RANKING_PADRAO -> rankingStrategy;
            case SORTEIO_TOTAL -> sorteioTotalStrategy;
            case SORTEIO_DIRIGIDO -> sorteioDirigidoStrategy;
            case POTES_MANUAIS -> potesManuaisStrategy;
        };
    }
}