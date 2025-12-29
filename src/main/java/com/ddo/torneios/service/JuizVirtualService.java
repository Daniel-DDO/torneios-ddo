package com.ddo.torneios.service;

import com.ddo.torneios.dto.DadosPartidaDTO;
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

    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            this.regulamentoCache = StreamUtils.copyToString(
                    regulamentoResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            System.out.println("Regulamento DDO carregado na memória: " + regulamentoCache.length() + " caracteres.");
        } catch (Exception e) {

            System.err.println("ALERTA: Arquivo 'regulamento_ddo.txt' não encontrado. O sistema iniciou sem as regras específicas.");
            this.regulamentoCache = "ERRO: O regulamento oficial não pôde ser carregado. Julgue o caso baseando-se apenas no bom senso, fair play e justiça desportiva comum.";
        }
    }

    public ReportPartida analisarDisputa(String partidaId, DadosPartidaDTO dadosDTO) {
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

            return reportPartidaRepository.save(report);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro no Tribunal Virtual: " + e.getMessage());
        }
    }

    private String montarPrompt(DadosPartidaDTO dados) {
        // Formata o prompt como se fosse um documento oficial
        return String.format("""
            ATUE COMO O JUIZ SUPREMO DA LIGA REAL DDO.
            
            1. CONTEXTO DA DISPUTA (AUTOS DO PROCESSO):
            -----------------------------------------------------
            MANDANTE:  %s (Jogando com: %s)
            VISITANTE: %s (Jogando com: %s)
            
            RELATO DO OCORRIDO:
            "%s"
            -----------------------------------------------------

            2. A LEI (REGULAMENTO OFICIAL VIGENTE - LEIA COM ATENÇÃO):
            -----------------------------------------------------
            %s
            -----------------------------------------------------

            3. SUA MISSÃO:
            Com base APENAS no regulamento acima (ignore regras da FIFA se contradizerem o texto), julgue o caso.
            - Se for caso de W.O. por atraso, verifique os 20 minutos (Art. 17).
            - Se for queda de conexão, verifique se o placar é mantido (Art. 20).
            - Se for uniforme igual, verifique a responsabilidade do mandante.

            4. FORMATO DE RESPOSTA (JSON OBRIGATÓRIO):
            {
                "veredito": "Veredito curto (Ex: Vitória de [Nome] por W.O.)",
                "explicacao": "Explicação técnica citando o Artigo X do regulamento.",
                "confiabilidade": Inteiro de 0 a 100
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
            throw new RuntimeException("A IA não retornou julgamento. Verifique filtros de segurança.");
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