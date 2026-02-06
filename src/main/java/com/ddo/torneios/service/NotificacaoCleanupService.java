package com.ddo.torneios.service;

import com.ddo.torneios.repository.NotificacaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificacaoCleanupService {

    @Autowired
    private NotificacaoRepository repository;

    //todos os dias às 03:00 da manhã
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void apagarNotificacoesAntigas() {
        LocalDateTime agora = LocalDateTime.now();

        LocalDateTime limiteGeral = agora.minusDays(15);
        repository.deleteByDataCriacaoBefore(limiteGeral);

        LocalDateTime limiteLeitura = agora.minusDays(1);
        repository.deleteByLidaTrueAndDataLeituraBefore(limiteLeitura);

        System.out.println("Limpeza de notificações realizada (Geral: 15 dias | Lidas: 24h).");
    }
}