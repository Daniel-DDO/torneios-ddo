package com.ddo.torneios.controller;

import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.model.JogadorClube;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.service.gerador.GeradorMataMataSorteioDirigidoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug/mata-mata")
public class DebugSorteioController {

    @Autowired
    private FaseTorneioRepository faseRepository;

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;

    @Autowired
    private GeradorMataMataSorteioDirigidoStrategy sorteioDirigidoStrategy;

    /**
     * Roda o sorteio dirigido em memória (NADA é salvo no banco) e devolve
     * a ordem/ranking usada e o resultado dos confrontos gerados,
     * pra comparar visualmente com o que foi gerado de verdade.
     */
    @GetMapping("/{faseId}/simular-sorteio-dirigido")
    @Transactional(readOnly = true)
    public ResponseEntity<?> simularSorteioDirigido(@PathVariable String faseId) {
        FaseTorneio fase = faseRepository.findById(faseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase não encontrada"));

        List<ParticipacaoFase> participantes = participacaoRepository.findByFase(fase);

        // Mapa auxiliar: jogadorClubeId -> posicaoClassificacao (pra exibir no resultado)
        Map<String, Integer> posicaoPorJogadorClube = participantes.stream()
                .collect(Collectors.toMap(p -> p.getJogadorClube().getId(), ParticipacaoFase::getPosicaoClassificacao));

        List<Partida> resultado = sorteioDirigidoStrategy.gerar(fase, participantes);

        List<Map<String, Object>> confrontos = resultado.stream()
                .collect(Collectors.groupingBy(Partida::getChaveIndex))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> montarConfronto(e.getKey(), e.getValue(), posicaoPorJogadorClube))
                .toList();

        List<Map<String, Object>> ordemUsada = participantes.stream()
                .sorted(Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> Map.<String, Object>of(
                        "posicao", p.getPosicaoClassificacao(),
                        "jogadorClubeId", p.getJogadorClube().getId(),
                        "jogador", p.getJogadorClube().getJogador().getNome(),
                        "clube", p.getJogadorClube().getClube().getNome()
                ))
                .toList();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ordemClassificacaoUsadaNoSorteio", ordemUsada);
        resposta.put("confrontos", confrontos);

        return ResponseEntity.ok(resposta);
    }

    private Map<String, Object> montarConfronto(Integer chaveIndex, List<Partida> jogos,
                                                Map<String, Integer> posicaoPorJogadorClube) {
        List<Map<String, Object>> jogosDTO = jogos.stream()
                .sorted(Comparator.comparing(p -> p.getTipoPartida().name()))
                .map(p -> {
                    JogadorClube mandante = p.getMandante();
                    JogadorClube visitante = p.getVisitante();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tipoPartida", p.getTipoPartida());
                    m.put("mandante", descreverJogador(mandante, posicaoPorJogadorClube));
                    m.put("visitante", descreverJogador(visitante, posicaoPorJogadorClube));
                    return m;
                })
                .toList();

        Map<String, Object> confronto = new LinkedHashMap<>();
        confronto.put("chaveIndex", chaveIndex);
        confronto.put("jogos", jogosDTO);
        return confronto;
    }

    private Map<String, Object> descreverJogador(JogadorClube jc, Map<String, Integer> posicoes) {
        if (jc == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jogador", jc.getJogador().getNome());
        m.put("clube", jc.getClube().getNome());
        m.put("posicaoClassificacaoOriginal", posicoes.get(jc.getId()));
        return m;
    }
}