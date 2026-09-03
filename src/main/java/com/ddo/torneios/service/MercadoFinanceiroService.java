package com.ddo.torneios.service;

import com.ddo.torneios.dto.MercadoStatusDTO;
import com.ddo.torneios.dto.ResumoEconomicoDTO;
import com.ddo.torneios.model.MercadoFinanceiroStatus;
import com.ddo.torneios.repository.ClubeRepository;
import com.ddo.torneios.repository.MercadoFinanceiroStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoFinanceiroService {

    private final ClubeRepository clubeRepository;
    private final MercadoFinanceiroStatusRepository statusRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private static final String API_URL_USD = "https://economia.awesomeapi.com.br/json/last/USD-BRL";
    private static final String API_URL_IPCA = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.433/dados/ultimos/1?formato=json";

    private static final BigDecimal VALOR_PISO_CLUBE = new BigDecimal("50000");
    private static final BigDecimal FATOR_CAOS = new BigDecimal("1.0");

    /**
     * Roda todos os dias à meia-noite, horário de Recife/São Paulo (explícito — evita
     * desalinhamento se o servidor rodar em outro timezone).
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void atualizarValorDeMercadoClubes() {
        log.info("Iniciando atualização diária do valor de mercado dos clubes...");

        MercadoFinanceiroStatus status = obterOuCriarStatus();
        status.setUltimaExecucao(LocalDateTime.now());

        try {
            String jsonResponse = restTemplate.getForObject(API_URL_USD, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode usdNode = root.path("USDBRL");

            if (usdNode.isMissingNode()) {
                throw new IllegalArgumentException("JSON inválido: Nó USDBRL não encontrado");
            }

            BigDecimal cotacaoAtual = new BigDecimal(usdNode.path("bid").asText());
            BigDecimal variacaoPercentual = new BigDecimal(usdNode.path("pctChange").asText())
                    .multiply(FATOR_CAOS);

            if (variacaoPercentual.compareTo(BigDecimal.ZERO) == 0) {
                log.info("Sem variação cambial detectada. Valores mantidos.");
                status.setUltimaExecucaoComSucesso(LocalDateTime.now());
                status.setUltimaCotacaoUsd(cotacaoAtual);
                status.setUltimaVariacaoUsdAplicada(BigDecimal.ZERO);
                status.setClubesAtualizadosUltimaExecucao(0);
                status.setUltimaExecucaoComErro(false);
                status.setUltimoErro(null);
                statusRepository.save(status);
                return;
            }

            BigDecimal fatorCorrecao = variacaoPercentual
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN)
                    .add(BigDecimal.ONE);

            log.info("Mercado Financeiro: Variação de {}%. Fator de correção global: {}", variacaoPercentual, fatorCorrecao);

            int atualizados = clubeRepository.aplicarFatorGlobal(fatorCorrecao, VALOR_PISO_CLUBE);

            status.setUltimaExecucaoComSucesso(LocalDateTime.now());
            status.setUltimaCotacaoUsd(cotacaoAtual);
            status.setUltimaVariacaoUsdAplicada(variacaoPercentual);
            status.setClubesAtualizadosUltimaExecucao(atualizados);
            status.setUltimaExecucaoComErro(false);
            status.setUltimoErro(null);
            statusRepository.save(status);

            log.info("Processo finalizado. {} clubes tiveram seus valores reajustados.", atualizados);

        } catch (Exception e) {
            log.error("CRÍTICO: Falha ao conectar com AwesomeAPI ou processar economia.", e);
            status.setUltimaExecucaoComErro(true);
            status.setUltimoErro(e.getMessage());
            statusRepository.save(status);
        }
    }

    /**
     * Roda no dia 10 às 03:00 (data usual de divulgação do IPCA pelo IBGE/BCB).
     * Aplica a inflação mensal como segundo fator de correção, independente do dólar.
     */
    @Scheduled(cron = "0 0 3 10 * *", zone = "America/Sao_Paulo")
    @Transactional
    public void atualizarValorDeMercadoPorInflacao() {
        log.info("Iniciando atualização mensal por IPCA...");

        MercadoFinanceiroStatus status = obterOuCriarStatus();

        try {
            String jsonResponse = restTemplate.getForObject(API_URL_IPCA, String.class);
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (!root.isArray() || root.isEmpty()) {
                throw new IllegalArgumentException("JSON inválido: resposta do BCB vazia");
            }

            BigDecimal variacaoIpca = new BigDecimal(root.get(0).path("valor").asText());

            if (variacaoIpca.compareTo(BigDecimal.ZERO) == 0) {
                log.info("IPCA sem variação neste mês. Valores mantidos.");
                return;
            }

            BigDecimal fatorCorrecao = variacaoIpca
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN)
                    .add(BigDecimal.ONE);

            int atualizados = clubeRepository.aplicarFatorGlobal(fatorCorrecao, VALOR_PISO_CLUBE);

            status.setUltimaExecucaoIpca(LocalDateTime.now());
            status.setUltimaVariacaoIpcaAplicada(variacaoIpca);
            statusRepository.save(status);

            log.info("IPCA aplicado: {}%. {} clubes reajustados.", variacaoIpca, atualizados);

        } catch (Exception e) {
            log.error("Falha ao aplicar reajuste por IPCA.", e);
        }
    }

    /**
     * Dispara manualmente a atualização por cotação do dólar, sem esperar a meia-noite.
     * Útil para diagnosticar se o job está funcionando, ou forçar um reajuste imediato.
     */
    @Transactional
    public void forcarAtualizacaoAgora() {
        atualizarValorDeMercadoClubes();
    }

    public MercadoStatusDTO consultarStatus() {
        MercadoFinanceiroStatus status = obterOuCriarStatus();
        return new MercadoStatusDTO(
                status.getUltimaExecucao(),
                status.getUltimaExecucaoComSucesso(),
                status.getUltimaVariacaoUsdAplicada(),
                status.getUltimaCotacaoUsd(),
                status.getClubesAtualizadosUltimaExecucao(),
                status.isUltimaExecucaoComErro(),
                status.getUltimoErro(),
                status.getUltimaExecucaoIpca(),
                status.getUltimaVariacaoIpcaAplicada()
        );
    }

    private MercadoFinanceiroStatus obterOuCriarStatus() {
        return statusRepository.findById("SINGLETON")
                .orElseGet(MercadoFinanceiroStatus::new);
    }

    public ResumoEconomicoDTO consultarSituacaoAtual() {
        try {
            String jsonResponse = restTemplate.getForObject(API_URL_USD, String.class);

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode usdNode = root.path("USDBRL");

            BigDecimal bid = new BigDecimal(usdNode.path("bid").asText());
            BigDecimal pctChange = new BigDecimal(usdNode.path("pctChange").asText());

            String tendencia;
            String mensagem;
            String cor;

            if (pctChange.compareTo(BigDecimal.ZERO) > 0) {
                tendencia = "ALTA";
                mensagem = "Mercado aquecido! A valorização do dólar está encarecendo os passes dos clubes.";
                cor = "VERDE";
            } else if (pctChange.compareTo(BigDecimal.ZERO) < 0) {
                tendencia = "BAIXA";
                mensagem = "Oportunidade de investimento! A queda cambial barateou o valor de mercado dos clubes.";
                cor = "VERMELHO";
            } else {
                tendencia = "ESTÁVEL";
                mensagem = "O mercado opera com estabilidade hoje. Nenhuma alteração brusca nos valores.";
                cor = "CINZA";
            }

            return ResumoEconomicoDTO.builder()
                    .cotacaoAtual(bid)
                    .variacaoPercentual(pctChange)
                    .tendencia(tendencia)
                    .mensagem(mensagem)
                    .corIndicativa(cor)
                    .build();

        } catch (Exception e) {
            log.error("Erro ao consultar resumo de mercado", e);
            return ResumoEconomicoDTO.builder()
                    .cotacaoAtual(BigDecimal.ZERO)
                    .variacaoPercentual(BigDecimal.ZERO)
                    .tendencia("INDISPONIVEL")
                    .mensagem("O sistema financeiro global está incomunicável no momento.")
                    .corIndicativa("CINZA")
                    .build();
        }
    }
}