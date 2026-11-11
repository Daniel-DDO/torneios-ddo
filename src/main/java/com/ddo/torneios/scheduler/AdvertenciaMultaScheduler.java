package com.ddo.torneios.scheduler;

import com.ddo.torneios.model.StatusAdvertencia;
import com.ddo.torneios.repository.AdvertenciaRepository;
import com.ddo.torneios.service.AdvertenciaService;
import com.ddo.torneios.util.RelogioBrasilia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AdvertenciaMultaScheduler {

    private static final int TAMANHO_LOTE = 200;

    private final AdvertenciaRepository advertenciaRepository;
    private final AdvertenciaService advertenciaService;

    public AdvertenciaMultaScheduler(AdvertenciaRepository advertenciaRepository,
                                     AdvertenciaService advertenciaService) {
        this.advertenciaRepository = advertenciaRepository;
        this.advertenciaService = advertenciaService;
    }

    @Scheduled(fixedDelay = 60000L)
    public void debitarMultasVencidas() {
        List<String> ids = advertenciaRepository.buscarIdsComMultaPendente(
                RelogioBrasilia.agora(), StatusAdvertencia.CANCELADA, PageRequest.of(0, TAMANHO_LOTE));

        for (String id : ids) {
            try {
                advertenciaService.debitarMulta(id);
            } catch (RuntimeException e) {
                log.error("Falha ao debitar multa da advertência {}", id, e);
            }
        }
    }
}
