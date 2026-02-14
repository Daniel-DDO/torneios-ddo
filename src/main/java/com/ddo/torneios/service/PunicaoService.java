package com.ddo.torneios.service;

import com.ddo.torneios.dto.PunicaoDTO;
import com.ddo.torneios.model.ParticipacaoFase;
import com.ddo.torneios.model.Punicao;
import com.ddo.torneios.repository.ParticipacaoFaseRepository;
import com.ddo.torneios.repository.PunicaoRepository;
import com.ddo.torneios.request.PunicaoRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PunicaoService {

    @Autowired
    private PunicaoRepository punicaoRepository;

    @Autowired
    private ParticipacaoFaseRepository participacaoFaseRepository; // Você precisará criar ou injetar este repositório

    @Transactional
    public PunicaoDTO aplicarPunicao(PunicaoRequest request) {
        ParticipacaoFase participacao = participacaoFaseRepository.findById(request.getParticipacaoFaseId())
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada: " + request.getParticipacaoFaseId()));

        Punicao punicao = new Punicao(participacao, request.getPontos(), request.getMotivo());
        Punicao punicaoSalva = punicaoRepository.save(punicao);

        int novaPontuacao = participacao.getPontos() + request.getPontos();
        participacao.setPontos(novaPontuacao);
        participacaoFaseRepository.save(participacao);

        return new PunicaoDTO(punicaoSalva);
    }

    @Transactional
    public void removerPunicao(String punicaoId) {
        Punicao punicao = punicaoRepository.findById(punicaoId)
                .orElseThrow(() -> new EntityNotFoundException("Punição não encontrada: " + punicaoId));

        ParticipacaoFase participacao = punicao.getParticipacaoFase();

        int pontuacaoRevertida = participacao.getPontos() - punicao.getPontos();
        participacao.setPontos(pontuacaoRevertida);
        participacaoFaseRepository.save(participacao);

        punicaoRepository.delete(punicao);
    }

    public List<PunicaoDTO> listarPorParticipacao(String participacaoFaseId) {
        return punicaoRepository.findByParticipacaoFaseIdOrderByDataAplicacaoDesc(participacaoFaseId)
                .stream()
                .map(PunicaoDTO::new)
                .toList();
    }
}