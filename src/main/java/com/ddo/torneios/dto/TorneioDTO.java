package com.ddo.torneios.dto;

import com.ddo.torneios.model.Torneio;

public record TorneioDTO(
        String id,
        String nome,
        String temporadaId,
        String temporadaNome,
        String competicaoId,
        String competicaoNome
) {
    public TorneioDTO(Torneio torneio) {
        this(
                torneio.getId(),
                torneio.getNome(),
                torneio.getTemporada().getId(),
                torneio.getTemporada().getNome(),
                torneio.getCompeticao().getId(),
                torneio.getCompeticao().getNome()
        );
    }
}