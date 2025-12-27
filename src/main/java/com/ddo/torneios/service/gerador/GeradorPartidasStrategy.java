package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.ParticipacaoFase;
import java.util.List;

public interface GeradorPartidasStrategy<T> {
    List<T> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes);
}