package com.ddo.torneios.service;

import com.ddo.torneios.model.*;
import java.util.List;

public interface GeradorPartidasStrategy {
    List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes);
}