package com.ddo.torneios.service;

import com.ddo.torneios.dto.ConquistaDashboardDTO;
import com.ddo.torneios.model.Conquista;
import com.ddo.torneios.repository.ConquistaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConquistaService {

    @Autowired
    private ConquistaRepository conquistaRepository;

    @Transactional(readOnly = true)
    public Optional<ConquistaDashboardDTO> buscarDestaque() {
        return conquistaRepository.findFirstByOrderByDataConquistaDesc()
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public List<ConquistaDashboardDTO> buscarUltimasConquistas() {
        return conquistaRepository.findTop10ByOrderByDataConquistaDesc()
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConquistaDashboardDTO> buscarPorJogador(String jogadorId) {
        return conquistaRepository.findByJogadorIdOrderByDataConquistaDesc(jogadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConquistaDashboardDTO> buscarPorClube(String clubeId) {
        return conquistaRepository.findByClubeIdOrderByDataConquistaDesc(clubeId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConquistaDashboardDTO> buscarPorTitulo(String tituloId) {
        return conquistaRepository.findByTituloIdOrderByDataConquistaDesc(tituloId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    private ConquistaDashboardDTO converterParaDTO(Conquista c) {
        return new ConquistaDashboardDTO(
                c.getId(),
                c.getTitulo().getId(),
                c.getTitulo().getNome(),
                c.getNomeEdicao(),
                c.getImagem(),
                c.getJogador().getId(),
                c.getJogador().getNome(),
                c.getJogador().getImagem(),
                c.getClube() != null ? c.getClube().getId() : null,
                c.getClube() != null ? c.getClube().getNome() : "Sem Clube",
                c.getClube() != null ? c.getClube().getSigla() : "N/A",
                c.getClube() != null ? c.getClube().getImagem() : null,

                c.getDataConquista()
        );
    }
}