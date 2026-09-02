package com.ddo.torneios.controller;

import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.request.BackfillHistoricoRequest;
import com.ddo.torneios.service.ClassificacaoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

// ENDPOINT TEMPORÁRIO - usado para corrigir participações que foram
// substituídas por torneio ANTES da correção do histórico existir.
// Remover essa classe depois do backfill.
@RestController
@RequestMapping("/admin/backfill/participacao-fase")
public class ParticipacaoFaseBackfillController {

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository;

    @Autowired
    private ClassificacaoService classificacaoService;

    @PostMapping("/{participacaoFaseId}/historico")
    public ResponseEntity<?> adicionarHistorico(@PathVariable String participacaoFaseId,
                                                @RequestBody BackfillHistoricoRequest request) {
        ParticipacaoFase participacao = participacaoFaseRepository.findById(participacaoFaseId)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada: " + participacaoFaseId));

        if (participacao.getHistoricoJogadorClubeIds() == null) {
            participacao.setHistoricoJogadorClubeIds(new ArrayList<>());
        }

        if (participacao.getHistoricoJogadorClubeIds().contains(request.jogadorClubeIdAntigo())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Esse id antigo já está registrado no histórico desta participação."));
        }

        participacao.getHistoricoJogadorClubeIds().add(request.jogadorClubeIdAntigo());
        participacaoFaseRepository.save(participacao);

        classificacaoService.recalcularETransmitir(participacao.getFase());

        return ResponseEntity.ok(Map.of(
                "mensagem", "Histórico adicionado e classificação recalculada.",
                "participacaoFaseId", participacaoFaseId,
                "historicoAtual", participacao.getHistoricoJogadorClubeIds()
        ));
    }

    @DeleteMapping("/{participacaoFaseId}/historico")
    public ResponseEntity<?> removerHistorico(@PathVariable String participacaoFaseId,
                                              @RequestBody BackfillHistoricoRequest request) {
        ParticipacaoFase participacao = participacaoFaseRepository.findById(participacaoFaseId)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada: " + participacaoFaseId));

        if (participacao.getHistoricoJogadorClubeIds() == null
                || !participacao.getHistoricoJogadorClubeIds().contains(request.jogadorClubeIdAntigo())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Esse id antigo não está registrado no histórico desta participação."));
        }

        participacao.getHistoricoJogadorClubeIds().remove(request.jogadorClubeIdAntigo());
        participacaoFaseRepository.save(participacao);

        classificacaoService.recalcularETransmitir(participacao.getFase());

        return ResponseEntity.ok(Map.of(
                "mensagem", "Histórico removido e classificação recalculada.",
                "participacaoFaseId", participacaoFaseId,
                "historicoAtual", participacao.getHistoricoJogadorClubeIds()
        ));
    }
}