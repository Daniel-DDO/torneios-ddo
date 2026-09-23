package com.ddo.torneios.scheduler;

import com.ddo.torneios.service.EmprestimoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmprestimoScheduler {

    @Autowired
    private EmprestimoService emprestimoService;

    /**
     * Roda todo dia às 03:00. Cobra qualquer parcela cujo vencimento já tenha
     * passado (>=), então mesmo que o job falhe um dia ele recupera no próximo.
     * Ajuste o cron/timezone conforme o servidor.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    public void cobrarParcelasVencidas() {
        try {
            int processadas = emprestimoService.processarParcelasVencidas();
            log.info("[EmprestimoScheduler] {} parcela(s) processada(s) automaticamente.", processadas);
        } catch (Exception e) {
            log.error("[EmprestimoScheduler] Falha ao processar parcelas vencidas. Use o endpoint de " +
                    "forçar recebimento (PROPRIETARIO) caso necessário.", e);
        }
    }
}