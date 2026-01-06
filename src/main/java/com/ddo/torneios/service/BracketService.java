package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.PartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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

        if (partidaRepository.existeJogoPendente(fase, etapa, chave)) {
            log.info("Confronto ainda não finalizado (aguardando volta ou outros jogos). Chave: {}", chave);
            return;
        }

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

        if (jogos.size() == 1) {
            return jogos.get(0).getVencedor();
        }

        Partida jogoIda = null;
        Partida jogoVolta = null;

        for (Partida p : jogos) {
            TipoPartida tp = p.getTipoPartida();
            if (tp == TipoPartida.MATA_MATA_IDA || tp == TipoPartida.FINAL_IDA) {
                jogoIda = p;
            } else if (tp == TipoPartida.MATA_MATA_VOLTA || tp == TipoPartida.FINAL_VOLTA) {
                jogoVolta = p;
            }
        }

        if (jogoIda == null || jogoVolta == null) {
            throw new IllegalStateException("Confronto ID " + chave + " possui 2 jogos mas os tipos não são IDA/VOLTA corretamente.");
        }

        int golsMandanteIda = jogoIda.getGolsMandante();
        int golsVisitanteIda = jogoIda.getGolsVisitante();

        int golsMandanteVolta = jogoVolta.getGolsMandante();
        int golsVisitanteVolta = jogoVolta.getGolsVisitante();

        int totalTimeA = golsMandanteIda + golsVisitanteVolta;

        int totalTimeB = golsVisitanteIda + golsMandanteVolta;

        if (totalTimeA > totalTimeB) return jogoIda.getMandante();
        if (totalTimeB > totalTimeA) return jogoIda.getVisitante();

        if (jogoVolta.houvePenaltis()) {
            return jogoVolta.getVencedor();
        }

        return null;
    }
}