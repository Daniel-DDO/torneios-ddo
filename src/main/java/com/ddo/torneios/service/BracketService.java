package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaBracketDTO;
import com.ddo.torneios.dto.PartidaBracketProjection;
import com.ddo.torneios.dto.PartidaConfrontoDTO;
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

    public Map<String, List<PartidaBracketDTO>> obterBracket(FaseTorneio fase) {
        List<PartidaBracketProjection> partidas = partidaRepository.buscarBracketPorFase(fase);

        List<PartidaBracketDTO> dtos = new ArrayList<>();

        for (PartidaBracketProjection p : partidas) {
            PartidaBracketProjection ida = null;

            if (isVolta(p.tipoPartida())) {
                ida = partidas.stream()
                        .filter(other ->
                                other.etapaMataMata() == p.etapaMataMata() &&
                                        Objects.equals(other.chaveIndex(), p.chaveIndex()) &&
                                        isIda(other.tipoPartida())
                        )
                        .findFirst()
                        .orElse(null);
            }

            dtos.add(montarDTO(p, ida));
        }

        return dtos.stream()
                .collect(Collectors.groupingBy(
                        PartidaBracketDTO::etapaMataMata,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(PartidaBracketDTO::chaveIndex))
                                        .collect(Collectors.toList())
                        )
                ));
    }

    private PartidaBracketDTO montarDTO(PartidaBracketProjection atual, PartidaBracketProjection ida) {
        boolean houvePenaltis = atual.penaltisMandante() != null && atual.penaltisVisitante() != null;

        return new PartidaBracketDTO(
                atual.id(),
                atual.etapaMataMata() != null ? atual.etapaMataMata().name() : null,
                atual.chaveIndex(),
                atual.tipoPartida() != null ? atual.tipoPartida().name() : null,
                atual.realizada(),
                atual.mandante(),
                atual.visitante(),
                atual.golsMandante(),
                atual.golsVisitante(),
                calcularAgregadoMandante(atual, ida),
                calcularAgregadoVisitante(atual, ida),
                atual.penaltisMandante(),
                atual.penaltisVisitante(),
                houvePenaltis
        );
    }

    private Integer calcularAgregadoMandante(PartidaBracketProjection atual, PartidaBracketProjection ida) {
        if (atual.golsMandante() == null) return null;
        if (ida == null || ida.golsVisitante() == null) return atual.golsMandante();

        String idaVisitanteId = ida.visitante() != null ? ida.visitante().id() : null;
        String atualMandanteId = atual.mandante() != null ? atual.mandante().id() : null;

        if (Objects.equals(idaVisitanteId, atualMandanteId)) {
            return atual.golsMandante() + ida.golsVisitante();
        }
        return atual.golsMandante() + ida.golsMandante();
    }

    private Integer calcularAgregadoVisitante(PartidaBracketProjection atual, PartidaBracketProjection ida) {
        if (atual.golsVisitante() == null) return null;
        if (ida == null || ida.golsMandante() == null) return atual.golsVisitante();

        String idaMandanteId = ida.mandante() != null ? ida.mandante().id() : null;
        String atualVisitanteId = atual.visitante() != null ? atual.visitante().id() : null;

        if (Objects.equals(idaMandanteId, atualVisitanteId)) {
            return atual.golsVisitante() + ida.golsMandante();
        }
        return atual.golsVisitante() + ida.golsVisitante();
    }

    @Transactional
    public void processarAvancoVencedor(Partida partidaFinalizada) {
        if (partidaFinalizada.getProximaPartida() == null) return;

        FaseTorneio fase = partidaFinalizada.getFase();
        FaseMataMata etapa = partidaFinalizada.getEtapaMataMata();
        Integer chave = partidaFinalizada.getChaveIndex();

        if (partidaRepository.existeJogoPendente(fase, etapa, chave)) {
            log.info("Confronto ainda não finalizado. Chave: {}", chave);
            return;
        }

        JogadorClube vencedor = calcularVencedorAgregado(fase, etapa, chave);
        if (vencedor == null) return;

        Partida proximaMestra = partidaFinalizada.getProximaPartida();

        List<Partida> proximosJogos = partidaRepository.findByFaseAndEtapaMataMataAndChaveIndex(
                fase, proximaMestra.getEtapaMataMata(), proximaMestra.getChaveIndex()
        );

        int slot = partidaFinalizada.getSlotNaProxima();

        for (Partida p : proximosJogos) {
            boolean isJogoIdaOuUnico = ehJogoIdaOuUnico(p.getTipoPartida());

            //Lógica Cruzada:
            //Slot 1: Mandante na IDA, Visitante na VOLTA
            //Slot 2: Visitante na IDA, Mandante na VOLTA

            if (slot == 1) {
                if (isJogoIdaOuUnico) {
                    configurarMandante(p, vencedor);
                } else {
                    configurarVisitante(p, vencedor);
                }
            } else {
                if (isJogoIdaOuUnico) {
                    configurarVisitante(p, vencedor);
                } else {
                    configurarMandante(p, vencedor);
                }
            }
            partidaRepository.save(p);
        }
    }

    private boolean ehJogoIdaOuUnico(TipoPartida tipo) {
        return tipo == TipoPartida.MATA_MATA_IDA ||
                tipo == TipoPartida.FINAL_IDA ||
                tipo == TipoPartida.MATA_MATA_UNICO ||
                tipo == TipoPartida.FINAL_UNICA;
    }

    private void configurarMandante(Partida p, JogadorClube time) {
        p.setMandante(time);
        if (p.getEtapaMataMata() != FaseMataMata.FINAL && time.getClube() != null) {
            p.setEstadio(time.getClube().getEstadio());
        }
    }

    private void configurarVisitante(Partida p, JogadorClube time) {
        p.setVisitante(time);
    }

    private JogadorClube calcularVencedorAgregado(FaseTorneio fase, FaseMataMata etapa, Integer chave) {
        List<Partida> partidas = partidaRepository.findByFaseAndEtapaMataMataAndChaveIndex(fase, etapa, chave);

        if (partidas == null || partidas.isEmpty()) return null;

        if (partidas.size() == 1) {
            Partida unica = partidas.get(0);
            return unica.getVencedor();
        }

        Partida ida = partidas.stream().filter(p -> isIda(p.getTipoPartida())).findFirst().orElse(null);
        Partida volta = partidas.stream().filter(p -> isVolta(p.getTipoPartida())).findFirst().orElse(null);

        if (ida == null || volta == null) return null;

        JogadorClube timeA = ida.getMandante();
        JogadorClube timeB = ida.getVisitante();

        int golsTimeA = (ida.getGolsMandante() != null) ? ida.getGolsMandante() : 0;
        int golsTimeB = (ida.getGolsVisitante() != null) ? ida.getGolsVisitante() : 0;

        if (volta.getMandante().equals(timeA)) {
            golsTimeA += (volta.getGolsMandante() != null) ? volta.getGolsMandante() : 0;
            golsTimeB += (volta.getGolsVisitante() != null) ? volta.getGolsVisitante() : 0;
        } else {
            golsTimeA += (volta.getGolsVisitante() != null) ? volta.getGolsVisitante() : 0;
            golsTimeB += (volta.getGolsMandante() != null) ? volta.getGolsMandante() : 0;
        }

        if (golsTimeA > golsTimeB) return timeA;
        if (golsTimeB > golsTimeA) return timeB;

        if (volta.houvePenaltis()) {
            var penaltis = volta.getPenaltis();

            if (penaltis.getGolsMandante() > penaltis.getGolsVisitante()) {
                return volta.getMandante();
            } else if (penaltis.getGolsVisitante() > penaltis.getGolsMandante()) {
                return volta.getVisitante();
            }
        }

        return null;
    }

    private boolean isIda(TipoPartida tp) {
        return tp == TipoPartida.MATA_MATA_IDA || tp == TipoPartida.FINAL_IDA;
    }

    private boolean isVolta(TipoPartida tp) {
        return tp == TipoPartida.MATA_MATA_VOLTA || tp == TipoPartida.FINAL_VOLTA;
    }

    public List<PartidaConfrontoDTO> obterDetalhesConfronto(FaseTorneio fase, FaseMataMata etapa, Integer chaveIndex) {
        return partidaRepository.buscarDetalhesConfronto(fase, etapa, chaveIndex);
    }
}