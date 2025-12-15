package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExportService {

    @Autowired
    private ClassificacaoService classificacaoService;

    public RelatorioFaseDTO prepararDadosExportacao(FaseTorneio fase) {

        List<LinhaClassificacaoDTO> tabelaOficial = classificacaoService.calcularClassificacao(fase);

        List<RodadaPdfDTO> rodadas = fase.getRodadas().stream()
                .sorted(Comparator.comparing(Rodada::getNumero))
                .map(r -> new RodadaPdfDTO(
                        r.getNumero(),
                        r.getPartidas().stream().map(this::toPartidaDTO).toList()
                )).toList();

        Map<String, List<PartidaPdfDTO>> mapa = new TreeMap<>();
        fase.getParticipacoes().forEach(p -> {
            String key = p.getJogadorClube().getClube().getNome() + " — " + p.getJogadorClube().getJogador().getNome();
            List<PartidaPdfDTO> partidasDoCara = new ArrayList<>();

            fase.getRodadas().forEach(r -> r.getPartidas().forEach(partida -> {
                if (partida.getMandante().equals(p.getJogadorClube()) ||
                        partida.getVisitante().equals(p.getJogadorClube())) {
                    partidasDoCara.add(toPartidaDTO(partida));
                }
            }));
            mapa.put(key, partidasDoCara);
        });

        List<AgendaJogadorDTO> agenda = mapa.entrySet().stream()
                .map(e -> new AgendaJogadorDTO(e.getKey(), e.getValue())).toList();

        return new RelatorioFaseDTO(
                fase.getTorneio().getNome(),
                fase.getNome(),
                tabelaOficial,
                rodadas,
                agenda
        );
    }

    private PartidaPdfDTO toPartidaDTO(Partida p) {
        return new PartidaPdfDTO(
                p.getMandante().getClube().getNome(),
                p.getMandante().getJogador().getNome(),
                p.getMandante().getClube().getImagem(),
                p.getVisitante().getClube().getNome(),
                p.getVisitante().getJogador().getNome(),
                p.getVisitante().getClube().getImagem(),
                p.getGolsMandante(),
                p.getGolsVisitante(),
                p.getEstadio(),
                p.isRealizada()
        );
    }
}