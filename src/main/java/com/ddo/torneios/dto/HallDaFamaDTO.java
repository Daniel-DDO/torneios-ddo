package com.ddo.torneios.dto;

import java.util.List;

public record HallDaFamaDTO(
        List<RecordeJogadorDTO> artilheiroMaximo,
        List<RecordeJogadorDTO> maisTitulos,
        List<RecordeJogadorDTO> maisFinais,
        List<RecordeJogadorDTO> maisPartidas,
        RecordeJogadorDTO melhorAproveitamento,
        List<RecordeTemporadaDTO> melhorAtaqueTemporada,
        List<RecordeTemporadaDTO> melhorDefesaTemporada,
        List<RecordePartidaDTO> partidaComMaisGols,
        List<RecordePartidaDTO> maiorGoleada,
        List<RecordeClubeDTO> clubeComMaisTitulos
) {}