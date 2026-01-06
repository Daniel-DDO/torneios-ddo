package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.dto.RodadaDTO;
import com.ddo.torneios.model.Rodada;
import com.ddo.torneios.model.StatusRodada;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.RodadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RodadaService {

    private final RodadaRepository rodadaRepository;
    private final PartidaRepository partidaRepository;

    @Transactional(readOnly = true)
    public Page<RodadaDTO> listarRodadasPaginadas(String faseId, Pageable pageable) {
        return rodadaRepository.buscarTodasPorFasePaginada(faseId, pageable)
                .map(RodadaDTO::new);
    }

    @Transactional(readOnly = true)
    public RodadaDTO buscarPorNumero(String faseId, Integer numero) {
        Rodada rodada = rodadaRepository.buscarPorNumero(faseId, numero)
                .orElseThrow(() -> new RuntimeException("Rodada " + numero + " não encontrada nesta fase."));
        return new RodadaDTO(rodada);
    }

    @Transactional(readOnly = true)
    public List<PartidaDTO> buscarPartidasPorJogador(String faseId, String jogadorId) {
        return partidaRepository.findPorFaseEJogador(faseId, jogadorId)
                .stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void atualizarStatusRodada(String rodadaId, StatusRodada novoStatus) {
        Rodada rodada = rodadaRepository.findById(rodadaId)
                .orElseThrow(() -> new RuntimeException("Rodada não encontrada"));

        rodada.setStatus(novoStatus);
        rodadaRepository.save(rodada);
    }
}