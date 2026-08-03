package com.ddo.torneios.controller;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.FaseTorneioRepository;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.service.gerador.GeradorMataMataSorteioDirigidoStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DebugSorteioController {

    @Autowired
    private FaseTorneioRepository faseRepository;

    @Autowired
    private ParticipacaoFaseRepository participacaoRepository;

    @Autowired
    private GeradorMataMataSorteioDirigidoStrategy sorteioDirigidoStrategy;

    /**
     * Simula o sorteio dirigido a partir dos classificados da fase de Liga informada,
     * SEM salvar nada no banco. A fase de mata-mata é montada em memória (transiente),
     * fixa em: SORTEIO_DIRIGIDO / OITAVAS / temJogoVolta=true / finalJogoUnico=true.
     *
     * faseAnteriorId = id da fase de Liga (de onde vêm os classificados)
     * quantidade     = opcional, default 16
     */
    @GetMapping("/debug/simular-sorteio-dirigido/{faseAnteriorId}")
    public Map<String, Object> simular(
            @PathVariable String faseAnteriorId,
            @RequestParam(defaultValue = "16") int quantidade) {

        FaseTorneio faseAnterior = faseRepository.findById(faseAnteriorId)
                .orElseThrow(() -> new IllegalArgumentException("Fase anterior não encontrada."));

        // Fase de mata-mata transiente, nunca persistida — só pra alimentar a strategy.
        FaseTorneio faseNova = new FaseTorneio();
        faseNova.setAlgoritmoMataMata(AlgoritmoGeracaoMataMata.SORTEIO_DIRIGIDO);
        faseNova.setFaseInicialMataMata(FaseMataMata.OITAVAS);
        faseNova.setTemJogoVolta(true);
        faseNova.setFinalJogoUnico(true);

        List<ParticipacaoFase> classificadosLiga = participacaoRepository
                .findByFaseIdOrderByPosicaoClassificacaoAsc(faseAnteriorId, PageRequest.of(0, quantidade));

        if (classificadosLiga.size() < quantidade) {
            throw new IllegalStateException(String.format(
                    "Fase anterior só tem %d participantes, esperado %d.", classificadosLiga.size(), quantidade));
        }

        // Monta participações TRANSIENTES (não persistidas) só pra alimentar a strategy,
        // exatamente como TransicaoFaseService.criarParticipacoesParaNovaFase faria.
        List<ParticipacaoFase> novasParticipacoes = new ArrayList<>();
        for (int i = 0; i < classificadosLiga.size(); i++) {
            ParticipacaoFase antiga = classificadosLiga.get(i);
            ParticipacaoFase transiente = new ParticipacaoFase();
            transiente.setFase(faseNova);
            transiente.setJogadorClube(antiga.getJogadorClube());
            transiente.setPosicaoClassificacao(i + 1);
            novasParticipacoes.add(transiente);
        }

        List<Map<String, Object>> classificadosDTO = new ArrayList<>();
        for (ParticipacaoFase p : novasParticipacoes) {
            classificadosDTO.add(Map.of(
                    "posicao", p.getPosicaoClassificacao(),
                    "nome", p.getJogadorClube().getJogador().getNome()
            ));
        }

        // Chama a strategy diretamente. Ela não salva nada — só monta os objetos Partida em memória.
        List<Partida> partidasGeradas = sorteioDirigidoStrategy.gerar(faseNova, novasParticipacoes);

        List<Map<String, Object>> bracketDTO = partidasGeradas.stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(
                            a.getChaveIndex() != null ? a.getChaveIndex() : 0,
                            b.getChaveIndex() != null ? b.getChaveIndex() : 0);
                    if (cmp != 0) return cmp;
                    return a.getTipoPartida().name().compareTo(b.getTipoPartida().name());
                })
                .map(p -> {
                    String mandanteNome = p.getMandante() != null ? p.getMandante().getJogador().getNome() : "(vazio)";
                    String visitanteNome = p.getVisitante() != null ? p.getVisitante().getJogador().getNome() : "(vazio)";
                    return Map.<String, Object>of(
                            "chaveIndex", p.getChaveIndex(),
                            "etapa", p.getEtapaMataMata(),
                            "tipoPartida", p.getTipoPartida(),
                            "mandante", mandanteNome,
                            "visitante", visitanteNome
                    );
                })
                .collect(Collectors.toList());

        return Map.of(
                "classificadosRecebidos", classificadosDTO,
                "bracketGerado", bracketDTO
        );
    }
}