package com.ddo.torneios.service;

import com.ddo.torneios.dto.ResumoEconomicoDTO;
import com.ddo.torneios.model.Clube;
import com.ddo.torneios.repository.ClubeRepository;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoFinanceiroService {

    private final ClubeRepository clubeRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://economia.awesomeapi.com.br/json/last/USD-BRL";

    private static final BigDecimal VALOR_PISO_CLUBE = new BigDecimal("50000");

    private static final BigDecimal FATOR_CAOS = new BigDecimal("1.0");

    /**
     * Roda todos os dias à meia-noite (00:00:00)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void atualizarValorDeMercadoClubes() {
        log.info("Iniciando atualização diária do valor de mercado dos clubes...");

        try {
            String jsonResponse = restTemplate.getForObject(API_URL, String.class);

            BigDecimal variacaoPercentual = obterVariacaoDaApi(jsonResponse);

            if (variacaoPercentual.compareTo(BigDecimal.ZERO) == 0) {
                log.info("Sem variação cambial detectada. Valores mantidos.");
                return;
            }

            variacaoPercentual = variacaoPercentual.multiply(FATOR_CAOS);

            BigDecimal fatorCorrecao = variacaoPercentual
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_EVEN)
                    .add(BigDecimal.ONE);

            log.info("Mercado Financeiro: Variação de {}%. Fator de correção global: {}", variacaoPercentual, fatorCorrecao);

            List<Clube> clubes = clubeRepository.findAll();
            int atualizados = 0;

            for (Clube clube : clubes) {
                if (clube.getValorAvaliado() == null) continue;

                BigDecimal valorAntigo = clube.getValorAvaliado();

                BigDecimal valorNovo = valorAntigo.multiply(fatorCorrecao)
                        .setScale(2, RoundingMode.HALF_EVEN);

                if (valorNovo.compareTo(VALOR_PISO_CLUBE) < 0) {
                    valorNovo = VALOR_PISO_CLUBE;
                }

                clube.setValorAvaliado(valorNovo);
                clube.atualizarLanceMinimo();
                atualizados++;
            }

            clubeRepository.saveAll(clubes);
            log.info("Processo finalizado. {} clubes tiveram seus valores reajustados.", atualizados);

        } catch (Exception e) {
            log.error("CRÍTICO: Falha ao conectar com AwesomeAPI ou processar economia.", e);
        }
    }

    private BigDecimal obterVariacaoDaApi(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            JsonNode usdNode = root.path("USDBRL");

            if (usdNode.isMissingNode()) {
                throw new IllegalArgumentException("JSON inválido: Nó USDBRL não encontrado");
            }

            String pctChangeStr = usdNode.path("pctChange").asText();

            return new BigDecimal(pctChangeStr);

        } catch (Exception e) {
            log.error("Erro ao fazer parse do JSON da API: {}", jsonBody, e);
            return BigDecimal.ZERO;
        }
    }

    public ResumoEconomicoDTO consultarSituacaoAtual() {
        try {
            String jsonResponse = restTemplate.getForObject(API_URL, String.class);

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