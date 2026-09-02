package com.ddo.torneios.service.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class GeminiProvedor implements ProvedorChatIA {

    @Value("${gemini.api.key.primary}")
    private String primaryKey;

    @Value("${gemini.api.key.secondary}")
    private String secondaryKey;

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    private static final long[] DELAYS_MS = {1000, 3000, 5000};

    private String getRotatedApiKey() {
        int index = requestCounter.getAndIncrement();
        return (index % 2 == 0) ? primaryKey : secondaryKey;
    }

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String gerarResposta(String prompt) throws Exception {
        RequestGemini requestGemini = new RequestGemini(List.of(
                new RequestGemini.Content(List.of(new RequestGemini.Part(prompt)))
        ));

        Exception ultimaFalha = null;
        int totalTentativas = DELAYS_MS.length + 1;

        for (int tentativa = 1; tentativa <= totalTentativas; tentativa++) {
            String apiKey = getRotatedApiKey();
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=%s",
                    apiKey
            );

            try {
                String responseJson = restTemplate.postForObject(url, requestGemini, String.class);

                JsonNode root = objectMapper.readTree(responseJson);
                if (!root.path("candidates").has(0)) {
                    throw new IllegalStateException("Gemini retornou sem candidates (possível bloqueio de conteúdo).");
                }
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            } catch (HttpServerErrorException e) {
                log.warn("Gemini indisponível (tentativa {}/{}): {}", tentativa, totalTentativas, e.getStatusCode());
                ultimaFalha = e;

                if (tentativa <= DELAYS_MS.length) {
                    long delay = DELAYS_MS[tentativa - 1];
                    log.info("Aguardando {}ms antes da próxima tentativa...", delay);
                    Thread.sleep(delay);
                }

            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                log.error("Modelo Gemini indisponível — verifique o nome do modelo em uso. Resposta: {}", e.getResponseBodyAsString());
                throw e;
            }
        }

        throw ultimaFalha;
    }

    @Override
    public String nome() {
        return "Gemini";
    }

    private record RequestGemini(List<Content> contents) {
        record Content(List<Part> parts) {}
        record Part(String text) {}
    }
}