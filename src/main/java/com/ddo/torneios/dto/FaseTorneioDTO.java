package com.ddo.torneios.dto;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.TipoTorneio;

public record FaseTorneioDTO(
        String id,
        String nome,
        Integer ordem,
        String torneioId,
        String torneioNome,
        TipoTorneio tipoTorneio,
        Integer numeroRodadas, //Null se for mata-mata
        String faseInicialMataMata, //Null se for pontos corridos
        Boolean temJogoVolta
) {
    public FaseTorneioDTO(FaseTorneio fase) {
        this(
                fase.getId(),
                fase.getNome(),
                fase.getOrdem(),
                fase.getTorneio().getId(),
                fase.getTorneio().getNome(),
                fase.getTipoTorneio(),
                fase.getNumeroRodadas(),
                fase.getFaseInicialMataMata() != null ? fase.getFaseInicialMataMata().name() : null,
                fase.getTemJogoVolta()
        );
    }
}