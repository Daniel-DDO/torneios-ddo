package com.ddo.torneios.service;

import com.ddo.torneios.dto.DadosPartidaDTO;
import com.ddo.torneios.dto.ReportPartidaDTO; // Importe o novo DTO
import com.ddo.torneios.model.Partida;
import com.ddo.torneios.model.ReportPartida;
import com.ddo.torneios.repository.PartidaRepository;
import com.ddo.torneios.repository.ReportPartidaRepository;
import com.ddo.torneios.request.JuizVirtualRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class JuizVirtualService {

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private ReportPartidaRepository reportPartidaRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("classpath:regulamento_ddo.txt")
    private Resource regulamentoResource;

    private String regulamentoCache;

    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("$")) {
            System.err.println("ERRO GRAVE: A API Key do Gemini não foi carregada corretamente!");
        }
        try {
            this.regulamentoCache = StreamUtils.copyToString(
                    regulamentoResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            this.regulamentoCache = "ERRO: O regulamento oficial não pôde ser carregado.";
        }
    }

    public ReportPartidaDTO analisarDisputa(String partidaId, DadosPartidaDTO dadosDTO) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        String promptFinal = montarPrompt(dadosDTO);

        JuizVirtualRequest request = new JuizVirtualRequest();
        request.setContents(List.of(
                new JuizVirtualRequest.Content(List.of(new JuizVirtualRequest.Part(promptFinal)))
        ));

        try {
            String url = String.format(API_URL_TEMPLATE, apiKey);
            String responseJson = restTemplate.postForObject(url, request, String.class);

            AnaliseIADTO analise = extrairResultado(responseJson);

            ReportPartida report = new ReportPartida();
            report.setPartida(partida);
            report.setRelatoAdmin(dadosDTO.getRelatoOcorrido());
            report.setAnaliseIA(analise.getExplicacao());
            report.setVereditoSugrido(analise.getVeredito());
            report.setNivelConfiabilidade(analise.getConfiabilidade());

            ReportPartida salvo = reportPartidaRepository.save(report);

            return new ReportPartidaDTO(salvo);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro no Tribunal Virtual: " + e.getMessage());
        }
    }

    private String montarPrompt(DadosPartidaDTO dados) {
        return String.format("""
            ATUE COMO O JUIZ SUPREMO DA LIGA REAL DDO.
            
            1. CONTEXTO DA DISPUTA:
            MANDANTE:  %s (Time: %s)
            VISITANTE: %s (Time: %s)
            RELATO: "%s"
            
            2. REGULAMENTO:
            %s

            3. REQUISITO:
            Julgue com base no regulamento. Responda APENAS o JSON abaixo:
            {
                "veredito": "Resumo curto",
                "explicacao": "Detalhes citando artigos",
                "confiabilidade": 0-100
            }
            """,
                dados.getNomeMandante(), dados.getTimeMandante(),
                dados.getNomeVisitante(), dados.getTimeVisitante(),
                dados.getRelatoOcorrido(),
                this.regulamentoCache
        );
    }

    private AnaliseIADTO extrairResultado(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);

        if (!root.path("candidates").has(0)) {
            throw new RuntimeException("A IA não retornou julgamento.");
        }

        String texto = root.path("candidates").get(0)
                .path("content").path("parts").get(0).path("text").asText();

        String jsonLimpo = texto.replaceAll("```json", "").replaceAll("```", "").trim();

        return objectMapper.readValue(jsonLimpo, AnaliseIADTO.class);
    }

    @Data
    private static class AnaliseIADTO {
        private String veredito;
        private String explicacao;
        private Integer confiabilidade;
    }
}