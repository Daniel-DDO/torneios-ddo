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

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Noticia> listarUltimas() {
        return noticiaRepository.findTop10ByOrderByDataCriacaoDesc();
    }

    public void gerarNoticiaSeRelevante(Partida partida) {
        log.info(">>> Iniciando análise de notícia para Partida ID: {}", partida.getId());

        if (partida == null || !partida.isRealizada()) {
            return;
        }

        Temporada temporadaAtual = partida.getFase().getTorneio().getTemporada();

        List<JogadorClube> top6Season = jogadorClubeRepository.findTop6ByTemporadaOrderByPontosCoeficienteDesc(temporadaAtual);

        List<Jogador> top10Legends = jogadorRepository.findTop10ByOrderByPontosCoeficienteDescTitulosDescFinaisDescVitoriasDesc();

        PerfilJogador perfilMandante = analisarPerfil(partida.getMandante(), top6Season, top10Legends);
        PerfilJogador perfilVisitante = analisarPerfil(partida.getVisitante(), top6Season, top10Legends);

        boolean interessante = isJogoInteressante(partida, perfilMandante, perfilVisitante);
        log.info("- A partida é interessante? {}", interessante ? "SIM" : "NÃO");

        if (!interessante) {
            return;
        }

        try {
            log.info("- Montando prompt e chamando Gemini...");
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
                log.info("- SUCESSO! Notícia salva: {}", mancheteJson.titulo);
            }

        } catch (Exception e) {
            log.error("- ERRO ao gerar notícia: ", e);
        }
    }

    private boolean isJogoInteressante(Partida p, PerfilJogador pm, PerfilJogador pv) {
        FaseMataMata etapa = p.getEtapaMataMata();

        if (etapa != null) {
            if (etapa == FaseMataMata.FINAL || etapa == FaseMataMata.SEMIFINAL) {
                log.info("Critério: Fase Decisiva ({})", etapa);
                return true;
            }

            if (etapa == FaseMataMata.QUARTAS) {
                Integer valorTorneio = 0;
                if (p.getFase().getTorneio().getCompeticao() != null) {
                    valorTorneio = p.getFase().getTorneio().getCompeticao().getValor();
                }

                if (valorTorneio != null && valorTorneio >= 80) {
                    log.info("Critério: Quartas de Final de Torneio Grande (Valor {})", valorTorneio);
                    return true;
                }
            }
        }

        if (pm.isRelevante() && pv.isRelevante()) {
            log.info("Critério: Choque de Titãs");
            return true;
        }

        int cartoesM = (p.getCartoesAmarelosMandante() != null ? p.getCartoesAmarelosMandante() : 0) +
                (p.getCartoesVermelhosMandante() != null ? p.getCartoesVermelhosMandante() : 0);
        int cartoesV = (p.getCartoesAmarelosVisitante() != null ? p.getCartoesAmarelosVisitante() : 0) +
                (p.getCartoesVermelhosVisitante() != null ? p.getCartoesVermelhosVisitante() : 0);

        if ((cartoesM + cartoesV) >= 5) {
            log.info("Critério: Jogo Violento ({} cartões)", cartoesM + cartoesV);
            return true;
        }

        int gM = p.getGolsMandante() != null ? p.getGolsMandante() : 0;
        int gV = p.getGolsVisitante() != null ? p.getGolsVisitante() : 0;
        if (Math.abs(gM - gV) >= 4) {
            log.info("Critério: Goleada");
            return true;
        }

        JogadorClube vencedor = p.getVencedor();
        if (vencedor != null) {
            boolean zebraMandante = pv.isRelevante() && !pm.isRelevante() && vencedor.getId().equals(p.getMandante().getId());
            boolean zebraVisitante = pm.isRelevante() && !pv.isRelevante() && vencedor.getId().equals(p.getVisitante().getId());
            if (zebraMandante || zebraVisitante) {
                log.info("Critério: Zebra");
                return true;
            }
        }

        return false;
    }

    private String montarPrompt(Partida p, PerfilJogador pm, PerfilJogador pv) {
        String dadosMandante = formatarDados(p.getMandante(), pm);
        String dadosVisitante = formatarDados(p.getVisitante(), pv);

        int cAmarelos = (p.getCartoesAmarelosMandante() != null ? p.getCartoesAmarelosMandante() : 0) +
                (p.getCartoesAmarelosVisitante() != null ? p.getCartoesAmarelosVisitante() : 0);
        int cVermelhos = (p.getCartoesVermelhosMandante() != null ? p.getCartoesVermelhosMandante() : 0) +
                (p.getCartoesVermelhosVisitante() != null ? p.getCartoesVermelhosVisitante() : 0);

        String placar = p.getGolsMandante() + " x " + p.getGolsVisitante();
        String torneio = p.getFase().getTorneio().getNome();

        String nomeFase = (p.getEtapaMataMata() != null) ? p.getEtapaMataMata().toString() : p.getFase().getNome();

        return String.format("""
            Você é um narrador de eSports sensacionalista dos Torneios DDO. Gere JSON (sem markdown) para uma notícia.
            
            Torneio: %s (%s)
            
            MANDANTE: %s
            VISITANTE: %s
            
            PLACAR FINAL: %s
            CARTÕES: %d Amarelos, %d Vermelhos.
            
            CONTEXTO:
            - [MVP]: Top 6 da Temporada (Melhor momento atual).
            - [LENDA]: Top 10 da História (Maior coeficiente acumulado e títulos).
            
            REGRAS DE DECISÃO "TIPO":
            - 'TITANS': Duelo entre duas Lendas ou MVPs.
            - 'ZEBRA': Lenda/MVP perdeu para um comum.
            - 'GOLEADA': Diferença de 4+ gols.
            - 'DECISAO': Final ou Semifinal.
            - 'BATALHA': Jogo com 5+ cartões no total.
            - 'JOGO_QUENTE': Outros casos interessantes.
            
            SAÍDA JSON:
            { "titulo": "Manchete curta e impactante (com emoji)", "mensagem": "Resumo jornalístico sensacionalista (max 120 chars)", "tipo": "..." }
            """,
                torneio, nomeFase,
                dadosMandante,
                dadosVisitante,
                placar, cAmarelos, cVermelhos);
    }

    private String limparMarkdown(String text) {
        if (text == null) return "{}";
        String limpo = text.replace("```json", "").replace("```", "").trim();
        int inicio = limpo.indexOf("{");
        int fim = limpo.lastIndexOf("}");
        if (inicio >= 0 && fim > inicio) {
            return limpo.substring(inicio, fim + 1);
        }
        return limpo;
    }

    private PerfilJogador analisarPerfil(JogadorClube jc, List<JogadorClube> topSeason, List<Jogador> topLegends) {
        if (jc == null || jc.getJogador() == null) return new PerfilJogador(false, false);

        boolean isTopSeason = topSeason.stream().anyMatch(top -> top.getId().equals(jc.getId()));
        boolean isLegend = topLegends.stream().anyMatch(lenda -> lenda.getId().equals(jc.getJogador().getId()));
        return new PerfilJogador(isTopSeason, isLegend);
    }

    private String formatarDados(JogadorClube jc, PerfilJogador perfil) {
        if (jc == null) return "Desconhecido";
        String nome = jc.getJogador().getNome();
        String clube = jc.getClube() != null ? jc.getClube().getNome() : "Time";

        StringBuilder status = new StringBuilder();
        if (perfil.isTopSeason) status.append("[MVP] ");
        if (perfil.isLegend) status.append("[LENDA] ");

        return String.format("%s (%s) %s", nome, clube, status.toString());
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