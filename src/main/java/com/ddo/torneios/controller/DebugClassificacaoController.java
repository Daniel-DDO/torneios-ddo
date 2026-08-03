package com.ddo.torneios.controller;

import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
public class DebugClassificacaoController {

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;

    @GetMapping("/debug/comparar-classificacao/{faseId}")
    public Map<String, Object> comparar(@PathVariable String faseId) {

        List<ParticipacaoFase> porPosicao = participacaoRepository
                .findByFaseIdOrderByPosicaoClassificacaoAsc(faseId, PageRequest.of(0, 16));

        List<ParticipacaoFase> porPontos = participacaoRepository
                .findByFaseIdOrderByPontosDescVitoriasDescSaldoGolsDescGolsProDesc(faseId);

        List<Map<String, Object>> listaPorPosicao = IntStream.range(0, porPosicao.size())
                .mapToObj(i -> {
                    ParticipacaoFase p = porPosicao.get(i);
                    return Map.<String, Object>of(
                            "indice", i + 1,
                            "nome", p.getJogadorClube().getJogador().getNome(),
                            "posicaoClassificacaoNoBanco", p.getPosicaoClassificacao(),
                            "pontos", p.getPontos()
                    );
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> listaPorPontos = IntStream.range(0, porPontos.size())
                .mapToObj(i -> {
                    ParticipacaoFase p = porPontos.get(i);
                    return Map.<String, Object>of(
                            "indice", i + 1,
                            "nome", p.getJogadorClube().getJogador().getNome(),
                            "posicaoClassificacaoNoBanco", p.getPosicaoClassificacao(),
                            "pontos", p.getPontos()
                    );
                })
                .collect(Collectors.toList());

        return Map.of(
                "porPosicaoClassificacaoAsc", listaPorPosicao,
                "porPontosCalculado", listaPorPontos
        );
    }
}