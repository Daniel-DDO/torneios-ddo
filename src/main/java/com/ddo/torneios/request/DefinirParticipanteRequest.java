package com.ddo.torneios.request;

import com.ddo.torneios.model.LadoPartida;

public record DefinirParticipanteRequest(String participacaoFaseId, LadoPartida lado) {}