package com.ddo.torneios.service;

import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.NoticiaRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class NoticiaService {

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Value("${gemini.api.key.noticia}")
    private String apiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Noticia> listarUltimas() {
        return noticiaRepository.findTop10ByOrderByDataCriacaoDesc();
    }

    public void gerarNoticiaSeRelevante(Partida partida) {
        if (partida == null || !partida.isRealizada() || partida.getMandante() == null || partida.getVisitante() == null) {
            return;
        }

        Temporada temporadaAtual = partida.getFase().getTorneio().getTemporada();
        List<JogadorClube> top5Season = jogadorClubeRepository.findTop5ByTemporadaOrderByPontosCoeficienteDesc(temporadaAtual);

        List<Jogador> top8Legends = jogadorRepository.findTop8ByOrderByTitulosDescFinaisDescVitoriasDesc();

        PerfilJogador perfilMandante = analisarPerfil(partida.getMandante(), top5Season, top8Legends);
        PerfilJogador perfilVisitante = analisarPerfil(partida.getVisitante(), top5Season, top8Legends);

        if (!isJogoInteressante(partida, perfilMandante, perfilVisitante)) {
            return;
        }

        try {
            String prompt = montarPrompt(partida, perfilMandante, perfilVisitante);
            String url = String.format(API_URL, apiKey);

            GeminiRequest request = new GeminiRequest(List.of(
                    new Content(List.of(new Part(prompt)))
            ));

            GeminiResponse response = restTemplate.postForObject(url, request, GeminiResponse.class);

            if (response != null && !response.candidates.isEmpty()) {
                String textoGerado = response.candidates.get(0).content.parts.get(0).text;
                String jsonLimpo = limparMarkdown(textoGerado);
                MancheteJson mancheteJson = objectMapper.readValue(jsonLimpo, MancheteJson.class);

                Noticia noticia = new Noticia();
                noticia.setTitulo(mancheteJson.titulo);
                noticia.setMensagem(mancheteJson.mensagem);
                noticia.setTipo(mancheteJson.tipo);
                noticia.setLinkPartida(frontendUrl + "/partida/" + partida.getId());

                noticiaRepository.save(noticia);
                log.info("Notícia gerada: {}", mancheteJson.titulo);
            }

        } catch (Exception e) {
            log.error("Erro ao gerar notícia IA: {}", e.getMessage());
        }
    }

    private PerfilJogador analisarPerfil(JogadorClube jc, List<JogadorClube> top5Season, List<Jogador> top8Legends) {
        boolean isTopSeason = top5Season.stream().anyMatch(top -> top.getId().equals(jc.getId()));
        boolean isLegend = top8Legends.stream().anyMatch(lenda -> lenda.getId().equals(jc.getJogador().getId()));

        return new PerfilJogador(isTopSeason, isLegend);
    }

    private boolean isJogoInteressante(Partida p, PerfilJogador pm, PerfilJogador pv) {
        if (pm.isRelevante() && pv.isRelevante()) return true;

        JogadorClube vencedor = p.getVencedor();
        if (vencedor != null) {
            boolean zebraMandante = pv.isRelevante() && !pm.isRelevante() && vencedor.getId().equals(p.getMandante().getId());
            boolean zebraVisitante = pm.isRelevante() && !pv.isRelevante() && vencedor.getId().equals(p.getVisitante().getId());
            if (zebraMandante || zebraVisitante) return true;
        }

        if (p.getFase().getNome().toLowerCase().contains("final")) return true;

        int saldo = Math.abs(p.getGolsMandante() - p.getGolsVisitante());
        if (saldo >= 4) return true;

        return false;
    }

    private String montarPrompt(Partida p, PerfilJogador pm, PerfilJogador pv) {
        String dadosMandante = formatarDados(p.getMandante(), pm);
        String dadosVisitante = formatarDados(p.getVisitante(), pv);

        String placar = p.getGolsMandante() + " x " + p.getGolsVisitante();
        String torneio = p.getFase().getTorneio().getNome();
        String fase = p.getFase().getNome();

        return String.format("""
            Você é um narrador de eSports, sensacionalista, dos Torneios DDO. Gere JSON (sem markdown) para uma notícia desta partida:
            
            Torneio: %s (%s)
            
            MANDANTE: %s
            VISITANTE: %s
            
            PLACAR FINAL: %s
            
            CONTEXTO DOS JOGADORES:
            - "MVP DA TEMPORADA": Está no Top 5 atual (melhor coeficiente).
            - "LENDA": Está no Top 8 histórico de títulos.
            
            REGRAS DE DECISÃO "TIPO":
            - 'TITANS': Duelo entre duas Lendas ou MVPs.
            - 'ZEBRA': Uma Lenda ou MVP perdeu para um desafiante comum.
            - 'GOLEADA': Diferença de 4+ gols.
            - 'DECISAO': Final de campeonato.
            - 'JOGO_QUENTE': Jogo equilibrado ou comum.
            
            SAÍDA JSON:
            { "titulo": "MAX 50 CHARS + EMOJI", "mensagem": "MAX 100 CHARS (Jornalístico)", "tipo": "..." }
            """,
                torneio, fase,
                dadosMandante,
                dadosVisitante,
                placar);
    }

    private String formatarDados(JogadorClube jc, PerfilJogador perfil) {
        String nome = jc.getJogador().getNome();
        String clube = jc.getClube() != null ? jc.getClube().getNome() : "Time";

        StringBuilder status = new StringBuilder();
        if (perfil.isTopSeason) status.append("[MVP DA TEMPORADA] ");
        if (perfil.isLegend) status.append("[LENDA] ");
        if (status.isEmpty()) status.append("(Desafiante)");

        return String.format("%s (%s) - Status: %s", nome, clube, status.toString());
    }

    private String limparMarkdown(String text) {
        if (text == null) return "{}";
        return text.replace("```json", "").replace("```", "").trim();
    }

    private record PerfilJogador(boolean isTopSeason, boolean isLegend) {
        public boolean isRelevante() { return isTopSeason || isLegend; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiRequest(List<Content> contents) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Content(List<Part> parts) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Part(String text) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiResponse {
        public List<Candidate> candidates;
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Candidate { public Content content; }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MancheteJson {
        public String titulo;
        public String mensagem;
        public String tipo;
    }
}