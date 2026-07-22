package com.ddo.torneios.service;

import com.ddo.torneios.dto.PaginacaoDTO;
import com.ddo.torneios.dto.PartidaDTO;
import com.ddo.torneios.dto.PartidaDetalheProjection;
import com.ddo.torneios.dto.PartidaHistoricoDTO;
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.repository.PartidaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;

    public PartidaDTO buscarPorId(String id) {
        PartidaDetalheProjection p = partidaRepository.buscarDetalhePorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada com id: " + id));
        return converterParaDTO(p);
    }

    private PartidaDTO converterParaDTO(PartidaDetalheProjection p) {
        boolean houvePenaltis = p.penaltisMandante() != null && p.penaltisVisitante() != null;

        return new PartidaDTO(
                p.id(),
                p.faseId(),
                p.rodadaId(),
                p.numeroRodada(),
                p.etapaMataMata() != null ? p.etapaMataMata().name() : null,
                p.chaveIndex(),
                p.dataHora(),
                p.estadio(),
                p.linkPartida(),
                p.mandante(),
                p.visitante(),
                p.golsMandante(),
                p.golsVisitante(),
                p.realizada(),
                p.wo(),
                p.houveProrrogacao(),
                houvePenaltis,
                p.penaltisMandante(),
                p.penaltisVisitante(),
                p.logEventos(),
                p.cartoesAmarelosMandante(),
                p.cartoesVermelhosMandante(),
                p.cartoesAmarelosVisitante(),
                p.cartoesVermelhosVisitante(),
                p.coeficienteMandante(),
                p.coeficienteVisitante(),
                p.tipoPartida() != null ? p.tipoPartida().name() : null,
                p.proximaPartidaId(),
                p.slotNaProxima(),
                p.receitaMandante() != null ? p.receitaMandante() : BigDecimal.ZERO,
                p.receitaVisitante() != null ? p.receitaVisitante() : BigDecimal.ZERO,
                p.golsMandante(),
                p.golsVisitante()
        );
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

    public PaginacaoDTO<PartidaHistoricoDTO> minhasPartidasFeitas(String jogadorId, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataHora"));
        Page<PartidaHistoricoDTO> resultado = partidaRepository.findPorJogadorIdEStatus(jogadorId, true, pageable);
        return montarPaginacao(resultado);
    }

    public PaginacaoDTO<PartidaHistoricoDTO> minhasPartidasParaFazer(String jogadorId, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "dataHora"));
        Page<PartidaHistoricoDTO> resultado = partidaRepository.findPorJogadorIdEStatus(jogadorId, false, pageable);
        return montarPaginacao(resultado);
    }

    private PaginacaoDTO<PartidaHistoricoDTO> montarPaginacao(Page<PartidaHistoricoDTO> pagina) {
        return new PaginacaoDTO<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.getSize(),
                pagina.isLast()
        );
    }

    public List<PartidaDTO> minhasPartidasPorTorneio(String jogadorId, String torneioId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "dataHora");

        return partidaRepository.findByJogadorAndTorneio(jogadorId, torneioId, sort)
                .stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }

    public List<PartidaDTO> minhasPartidasPorFase(String jogadorId, String faseId) {
        Sort sort = Sort.by(Sort.Direction.DESC, "dataHora");

        return partidaRepository.findByJogadorAndFase(jogadorId, faseId, sort)
                .stream()
                .map(PartidaDTO::new)
                .collect(Collectors.toList());
    }
}