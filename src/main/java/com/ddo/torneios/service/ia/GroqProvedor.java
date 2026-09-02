package com.ddo.torneios.service.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GroqProvedor implements ProvedorChatIA {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String gerarResposta(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.4
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        String responseJson = restTemplate.postForObject(API_URL, request, String.class);

        JsonNode root = objectMapper.readTree(responseJson);
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    @Override
    public String nome() {
        return "Groq";
    }
}