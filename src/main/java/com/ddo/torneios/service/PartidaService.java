package com.ddo.torneios.service;

import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.repository.PartidaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;

    public PartidaDTO buscarPorId(String id) {
        return partidaRepository.findById(id)
                .map(PartidaDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada com id: " + id));
    }

    public List<PartidaDTO> listarPorFase(String faseId) {
        return partidaRepository.findByFaseId(faseId).stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }

    public List<PartidaDTO> listarPorRodada(String rodadaId) {
        return partidaRepository.findByRodadaId(rodadaId).stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public PartidaDTO agendarPartida(String partidaId, LocalDateTime dataHora, String estadio, String linkPartida) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        if (dataHora != null) partida.setDataHora(dataHora);
        if (estadio != null && !estadio.isBlank()) partida.setEstadio(estadio);
        if (linkPartida != null && !linkPartida.isBlank()) partida.setLinkPartida(linkPartida);

        Partida salva = partidaRepository.save(partida);
        return new PartidaDTO(salva);
    }

    @Transactional
    public void resetarStatusPartida(String partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada"));

        partida.setRealizada(false);
        partida.setWo(false);
        partida.setGolsMandante(null);
        partida.setGolsVisitante(null);
        partida.setCoeficienteMandante(null);
        partida.setCoeficienteVisitante(null);

        //Aqui tem q chamar um serviço para desfazer a pontuação

        partidaRepository.save(partida);
    }

    public List<PartidaDTO> buscarAutocomplete(String termo) {
        if (termo == null || termo.trim().length() < 2) {
            return Collections.emptyList();
        }

        return partidaRepository.buscarAutocomplete(termo.trim(), PageRequest.of(0, 10, Sort.by("dataHora").descending()))
                .stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }
}