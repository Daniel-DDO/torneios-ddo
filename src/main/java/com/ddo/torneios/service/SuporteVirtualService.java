package com.ddo.torneios.service;

import com.ddo.torneios.dto.ChatRequestDTO;
import com.ddo.torneios.dto.MensagemChatDTO;
import com.ddo.torneios.dto.SuporteDTO;
import com.ddo.torneios.model.RegulamentoComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SuporteVirtualService {

    @Autowired
    private RegulamentoComponent regulamentoComponent;

    @Value("${gemini.api.key.primary}")
    private String primaryKey;

    @Value("${gemini.api.key.secondary}")
    private String secondaryKey;

    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getRotatedApiKey() {
        int index = requestCounter.getAndIncrement();
        return (index % 2 == 0) ? primaryKey : secondaryKey;
    }

    public SuporteDTO responderComContexto(ChatRequestDTO requestDTO) {
        String apiKeyAtual = getRotatedApiKey();

        String prompt = montarPromptComHistorico(requestDTO.historico(), requestDTO.novaPergunta());

        RequestGemini requestGemini = new RequestGemini(List.of(
                new RequestGemini.Content(List.of(new RequestGemini.Part(prompt)))
        ));

        try {
            String url = String.format(API_URL_TEMPLATE, apiKeyAtual);
            String responseJson = restTemplate.postForObject(url, requestGemini, String.class);
            String respostaTexto = extrairTexto(responseJson);

            return new SuporteDTO(requestDTO.novaPergunta(), respostaTexto);

        } catch (Exception e) {
            log.error("Erro no Chat Suporte (Key final: ...{})", apiKeyAtual.substring(Math.max(0, apiKeyAtual.length() - 4)), e);
            throw new RuntimeException("O suporte está indisponível no momento.");
        }
    }

    private String montarPromptComHistorico(List<MensagemChatDTO> historico, String novaPergunta) {
        String regulamento = regulamentoComponent.getTextoRegulamento();

        String historicoTexto = "";
        if (historico != null && !historico.isEmpty()) {
            historicoTexto = historico.stream()
                    .map(msg -> (msg.role().equalsIgnoreCase("user") ? "JOGADOR: " : "VOCÊ (SUPORTE): ") + msg.texto())
                    .collect(Collectors.joining("\n"));
        }

        return String.format("""
            Você é o SUPORTE VIRTUAL INTELIGENTE DOS TORNEIOS DDO.
            Sua função é tirar dúvidas e conversar com os jogadores de forma educada, clara e objetiva.
            
            === BASE DE CONHECIMENTO (REGULAMENTO OFICIAL) ===
            %s
            ==================================================

            === HISTÓRICO DA CONVERSA ATUAL ===
            %s
            ===================================
            
            PERGUNTA ATUAL DO JOGADOR:
            "%s"
            
            DIRETRIZES DE RESPOSTA:
            1. Use o HISTÓRICO para entender o contexto (ex: se ele disser "e sobre aquilo?", refira-se ao que foi dito antes).
            2. Responda APENAS com base no REGULAMENTO acima.
            3. Se a pergunta for social (ex: "oi", "obrigado"), seja educado, mas lembre que você é o suporte da Liga.
            4. Se a informação não estiver no regulamento, diga que não sabe e oriente procurar um administrador.
            """,
                regulamento,
                historicoTexto.isEmpty() ? "Nenhuma mensagem anterior." : historicoTexto,
                novaPergunta
        );
    }

    private String extrairTexto(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        if (!root.path("candidates").has(0)) return "Não entendi. Pode reformular?";
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    private record RequestGemini(List<Content> contents) {
        record Content(List<Part> parts) {}
        record Part(String text) {}
    }
}