package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HallDaFamaService {

    private final JogadorRepository jogadorRepository;
    private final JogadorClubeRepository jogadorClubeRepository;
    private final PartidaRepository partidaRepository;
    private final ClubeRepository clubeRepository;

    private static final int MINIMO_PARTIDAS_APROVEITAMENTO = 10;
    private static final int MINIMO_PARTIDAS_DEFESA = 5;

    public HallDaFamaDTO obterHallDaFama() {
        RecordeJogadorDTO aproveitamento = jogadorRepository
                .findMelhorAproveitamento(MINIMO_PARTIDAS_APROVEITAMENTO)
                .map(p -> new RecordeJogadorDTO(
                        p.getJogadorId(), p.getJogadorNome(), p.getJogadorImagem(),
                        p.getAproveitamento() + "%", p.getPartidasJogadas()))
                .orElse(null);

        return new HallDaFamaDTO(
                jogadorRepository.findArtilheiroMaximo(),
                jogadorRepository.findMaisTitulos(),
                jogadorRepository.findMaisFinais(),
                jogadorRepository.findMaisPartidas(),
                aproveitamento,
                jogadorClubeRepository.findMelhorAtaqueTemporada(),
                jogadorClubeRepository.findMelhorDefesaTemporada(MINIMO_PARTIDAS_DEFESA),
                partidaRepository.findPartidaComMaisGols(),
                partidaRepository.findMaiorGoleada(),
                clubeRepository.findClubeComMaisTitulos()
        );
    }
}
