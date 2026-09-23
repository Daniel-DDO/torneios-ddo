package com.ddo.torneios.service;

import com.ddo.torneios.repository.JogadorRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantém em memória a média global de (cartões amarelos + vermelhos) por
 * partida jogada entre todos os jogadores. Usada pra penalizar o limite de
 * empréstimo de quem tem cartão muito acima da média.
 *
 * Mesmo padrão do EstiloGlobalCache já usado no projeto: evita recalcular
 * essa agregação a cada checagem de elegibilidade.
 */
@Slf4j
@Component
public class CartaoGlobalCache {

    @Autowired
    private JogadorRepository jogadorRepository;

    private volatile double mediaCartoesPorPartida = 0.0;

    @PostConstruct
    public void carregarInicial() {
        atualizar();
    }

    /** Atualiza a média 1x por dia. Ajuste o cron se quiser mais/menos frequente. */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Sao_Paulo")
    public void atualizar() {
        try {
            Double media = jogadorRepository.buscarMediaCartoesPorPartida();
            this.mediaCartoesPorPartida = media != null ? media : 0.0;
            log.info("[CartaoGlobalCache] Média global de cartões/partida atualizada: {}", mediaCartoesPorPartida);
        } catch (Exception e) {
            log.error("[CartaoGlobalCache] Falha ao atualizar média global de cartões.", e);
        }
    }

    public double obterMediaCartoesPorPartida() {
        return mediaCartoesPorPartida;
    }
}