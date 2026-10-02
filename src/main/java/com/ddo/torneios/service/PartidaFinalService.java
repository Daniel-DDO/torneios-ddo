package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaFinalDTO;
import com.ddo.torneios.model.TipoPartida;
import com.ddo.torneios.repository.PartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartidaFinalService {

    private final PartidaRepository partidaRepository;

    @Transactional(readOnly = true)
    public Page<PartidaFinalDTO> listarFinais(Pageable pageable) {
        return partidaRepository.buscarFinais(TipoPartida.finais(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PartidaFinalDTO> listarFinaisRealizadas(Pageable pageable) {
        return partidaRepository.buscarFinaisRealizadas(TipoPartida.finais(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PartidaFinalDTO> listarFinaisPendentes(Pageable pageable) {
        return partidaRepository.buscarFinaisPendentes(TipoPartida.finais(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PartidaFinalDTO> listarFinaisDoJogador(String jogadorId, Pageable pageable) {
        return partidaRepository.buscarFinaisPorJogador(TipoPartida.finais(), jogadorId, pageable);
    }
}