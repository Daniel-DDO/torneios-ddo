package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.PartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BracketService {

    private final PartidaRepository partidaRepository;

    public Map<String, List<PartidaDTO>> obterBracket(FaseTorneio fase) {
        List<Partida> todasPartidas = partidaRepository.findByFase(fase);

        return todasPartidas.stream()
                .filter(p -> p.getEtapaMataMata() != null)
                .map(PartidaDTO::new)
                .collect(Collectors.groupingBy(
                        PartidaDTO::etapaMataMata,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(PartidaDTO::chaveIndex))
                                        .collect(Collectors.toList())
                        )
                ));
    }

    @Transactional
    public void processarAvancoVencedor(Partida partidaFinalizada) {
        if (partidaFinalizada.getProximaPartida() == null) return;

        FaseTorneio fase = partidaFinalizada.getFase();
        FaseMataMata etapa = partidaFinalizada.getEtapaMataMata();
        Integer chave = partidaFinalizada.getChaveIndex();

        boolean completo = partidaRepository.isConfrontoCompleto(fase, etapa, chave);
        if (!completo) return;

        JogadorClube vencedor = calcularVencedorAgregado(fase, etapa, chave);
        if (vencedor == null) return;

        Partida proximaMestra = partidaFinalizada.getProximaPartida();
        List<Partida> proximosJogos = partidaRepository.findByFaseAndEtapaMataMataAndChaveIndex(
                fase, proximaMestra.getEtapaMataMata(), proximaMestra.getChaveIndex()
        );

        for (Partida p : proximosJogos) {
            if (partidaFinalizada.getSlotNaProxima() == 1) {
                p.setMandante(vencedor);
            } else {
                p.setVisitante(vencedor);
            }
            partidaRepository.save(p);
        }
    }

    private JogadorClube calcularVencedorAgregado(FaseTorneio fase, FaseMataMata etapa, Integer chave) {
        List<Partida> jogos = partidaRepository.findByFaseAndEtapaMataMataAndChaveIndex(fase, etapa, chave);

        if (jogos.size() == 1) return jogos.get(0).getVencedor();

        Partida j1 = jogos.get(0);
        Partida j2 = jogos.get(1);

        int golsMandanteJ1 = j1.getGolsMandante() != null ? j1.getGolsMandante() : 0;
        int golsVisitanteJ1 = j1.getGolsVisitante() != null ? j1.getGolsVisitante() : 0;
        int golsMandanteJ2 = j2.getGolsMandante() != null ? j2.getGolsMandante() : 0;
        int golsVisitanteJ2 = j2.getGolsVisitante() != null ? j2.getGolsVisitante() : 0;

        int totalTimeA = golsMandanteJ1 + golsVisitanteJ2;
        int totalTimeB = golsVisitanteJ1 + golsMandanteJ2;

        if (totalTimeA > totalTimeB) return j1.getMandante();
        if (totalTimeB > totalTimeA) return j1.getVisitante();

        if (j2.houvePenaltis()) {
            return j2.getVencedor();
        }

        return null;
    }
}