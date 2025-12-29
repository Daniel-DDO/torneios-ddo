package com.ddo.torneios.service;


import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.ReportPartida;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.ReportPartidaRepository;
import com.ddo.torneios.request.JuizVirtualRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class JuizVirtualService {

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private ReportPartidaRepository reportPartidaRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REGULAMENTO_TEXTO = """
            REGULAMENTO DO TORNEIO:
            1. Tolerância de atraso: 15 minutos. Após isso, é W.O.
            2. Queda de conexão: Se ocorrer antes dos 10min de jogo, reinicia 0x0. Se depois, mantém placar e tempo restante.
            3. Jogador não responde: Se um jogador estiver online no sistema mas não responder no chat por 10min, perde por W.O.
            4. Duplo W.O: Se ambos falharem, sorteio define quem avança, mas ambos não pontuam ranking.
            """;

    public ReportPartida analisarProblema(String partidaId, String relatoAdmin) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        String prompt = montarPrompt(relatoAdmin);

        JuizVirtualRequest request = new JuizVirtualRequest();
        request.setContents(List.of(
                new JuizVirtualRequest.Content(List.of(new JuizVirtualRequest.Part(prompt)))
        ));

        try {
            String url = String.format(API_URL_TEMPLATE, apiKey);
            String responseJson = restTemplate.postForObject(url, request, String.class);

            AnaliseIADTO analise = extrairAnaliseDaResposta(responseJson);

            ReportPartida report = new ReportPartida();
            report.setPartida(partida);
            report.setRelatoAdmin(relatoAdmin);
            report.setAnaliseIA(analise.getExplicacao());
            report.setVereditoSugrido(analise.getVeredito());
            report.setNivelConfiabilidade(analise.getConfiabilidade());

            return reportPartidaRepository.save(report);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao consultar Juiz Virtual: " + e.getMessage());
        }
    }

    private String montarPrompt(String relato) {
        return String.format("""
                Você é o Juiz Supremo de um torneio. Analise o caso abaixo estritamente pelo regulamento.
                
                REGULAMENTO:
                %s
                
                RELATO DO ADMIN:
                "%s"
                
                Sua resposta deve ser EXCLUSIVAMENTE um JSON válido (sem markdown, sem ```json) com os campos:
                {
                    "veredito": "Resumo curto da decisão (ex: Vitória Jogador A)",
                    "explicacao": "Explicação detalhada citando a regra usada",
                    "confiabilidade": Inteiro de 0 a 100 indicando certeza
                }
                """, REGULAMENTO_TEXTO, relato);
    }

    private AnaliseIADTO extrairAnaliseDaResposta(String googleResponseJson) throws Exception {
        JsonNode root = objectMapper.readTree(googleResponseJson);

        String textoDaIA = root.path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        String jsonLimpo = textoDaIA.replace("```json", "").replace("```", "").trim();

        return objectMapper.readValue(jsonLimpo, AnaliseIADTO.class);
    }

    @Data
    private static class AnaliseIADTO {
        private String veredito;
        private String explicacao;
        private Integer confiabilidade;
    }
}