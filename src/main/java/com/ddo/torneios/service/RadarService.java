package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.*;
import com.ddo.torneios.service.radar.RadarAtributosCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RadarService {

    private static final int MINIMO_PARTIDAS_PARA_MEDIA_LIGA = 5;

    private final JogadorRepository jogadorRepository;
    private final RadarAtributosCalculator calculator = new RadarAtributosCalculator();

    @Transactional(readOnly = true)
    public RadarComparacaoDTO gerarRadar(String id1, String id2) {
        LigaMediasDTO mediasLiga = buscarMediasLiga();

        RadarJogadorDTO radarJ1 = montarRadarJogador(buscarOuFalhar(id1), mediasLiga);

        RadarJogadorDTO radarJ2 = (id2 != null && !id2.isBlank())
                ? montarRadarJogador(buscarOuFalhar(id2), mediasLiga)
                : null;

        return new RadarComparacaoDTO(radarJ1, radarJ2);
    }

    private LigaMediasDTO buscarMediasLiga() {
        LigaMediasProjection p = jogadorRepository.buscarMediasLigaNative(MINIMO_PARTIDAS_PARA_MEDIA_LIGA);
        return new LigaMediasDTO(
                p.getMediaGolsProPorJogo() != null ? p.getMediaGolsProPorJogo() : 0.0,
                p.getMediaGolsContraPorJogo() != null ? p.getMediaGolsContraPorJogo() : 0.0,
                p.getMediaCartoesPorJogo() != null ? p.getMediaCartoesPorJogo() : 0.0,
                p.getMediaJogos() != null ? p.getMediaJogos() : 0.0
        );
    }

    private JogadorDTO buscarOuFalhar(String id) {
        return jogadorRepository.buscarJogadorDtoPorId(id)
                .orElseThrow(() -> new JogadorNaoEncontradoException(id));
    }

    private RadarJogadorDTO montarRadarJogador(JogadorDTO j, LigaMediasDTO mediasLiga) {
        AtributosRadarDTO atributos = calculator.calcular(j, mediasLiga);
        return new RadarJogadorDTO(j.id(), j.nome(), j.imagem(), atributos);
    }
}