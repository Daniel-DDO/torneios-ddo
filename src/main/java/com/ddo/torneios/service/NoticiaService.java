package com.ddo.torneios.service;

import com.ddo.torneios.dto.PlacarIdaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.JogadorClubeRepository;
import com.ddo.torneios.repository.JogadorRepository;
import com.ddo.torneios.repository.NoticiaRepository;
import com.ddo.torneios.repository.PartidaRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class NoticiaService {

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private JogadorClubeRepository jogadorClubeRepository;

    @Autowired
    private JogadorRepository jogadorRepository;

    @Autowired
    private DiscordNotificationService discordService;

    @Autowired
    private PartidaRepository partidaRepository;

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

    @Async
    @Transactional
    public void gerarNoticiaSeRelevante(Partida partidaRecebida) {
        log.info(">>> Iniciando análise de notícia para Partida ID: {}", partidaRecebida.getId());

        if (partidaRecebida == null || !partidaRecebida.isRealizada()) {
            return;
        }

        Partida partida = partidaRepository.findById(partidaRecebida.getId()).orElse(null);
        if (partida == null) {
            log.warn("- Partida {} não encontrada ao processar notícia assíncrona.", partidaRecebida.getId());
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

                discordService.enviarNoticia(noticia);
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
        String contextoPartida = montarContextoPartida(p);

        return String.format("""
        Você é um narrador de eSports sensacionalista dos Torneios DDO. Gere APENAS um JSON (sem markdown, sem crases, sem texto fora do JSON) para uma notícia sobre a partida abaixo.

        Torneio: %s (%s)

        MANDANTE: %s
        VISITANTE: %s

        PLACAR DESTA PARTIDA: %s
        %s
        CARTÕES: %d Amarelos, %d Vermelhos.

        CONTEXTO DE ANÁLISE (uso interno — NÃO escreva estas palavras/siglas no texto final):
        - "destaque da temporada" = jogador entre os 6 melhores da temporada atual.
        - "consagrado/histórico" = jogador entre os 10 maiores nomes da história do torneio.
        Descreva esse status com suas próprias palavras, variando a cada notícia (ex: "um dos nomes mais fortes do momento", "veterano respeitado", "referência da competição", "um dos gigantes da história"). NUNCA use literalmente as palavras "MVP", "LENDA", "TITÃ" ou "TOP 6" como rótulo fixo no título ou mensagem.

        TOM (sensacionalista e engraçado — capriche!):
        - Pode e deve usar CAIXA ALTA pra dar ênfase em pontos-chave (um placar, um feito, uma zoeira), mas não a manchete inteira em maiúsculas toda vez — varie ONDE e QUANTO usa caps a cada notícia.
        - Pode usar humor, ironia, deboche leve e exagero teatral típico de manchete de fofoca esportiva.
        - PROIBIDO repetir sempre a mesma fórmula de exagero. Alterne entre: caps parcial, gírias, comparações inusitadas, trocadilhos, hipérboles diferentes a cada vez. Se a última notícia usou "MASSACROU" em caps, a próxima NÃO pode repetir esse mesmo recurso — troque a palavra E a forma de enfatizar.
        - Varie o vocabulário: não repita sempre as mesmas palavras-chave ("duelo", "confronto", "titãs", "gigantes", "massacrou", "doutrinou") em notícias diferentes — escolha sinônimos e formas diferentes de dizer a mesma coisa a cada geração.

        REGRA DE PRECISÃO (não abrir mão disso mesmo sendo engraçado):
        - Baseie-se ESTRITAMENTE no contexto real da partida descrito abaixo (se é ida, volta, jogo único, se houve prorrogação, pênaltis, agregado). Não afirme que um time "está classificado", "vai à final" ou "é campeão" a menos que o contexto confirme que esta partida decidiu isso. Pode ser engraçado sendo impreciso? Não — a piada é na FORMA, o FATO tem que estar certo.

        CONTEXTO REAL DA PARTIDA:
        %s

        REGRAS DE DECISÃO "TIPO":
        - 'TITANS': Duelo entre duas Lendas ou MVPs.
        - 'ZEBRA': Lenda/MVP perdeu para um comum.
        - 'GOLEADA': Diferença de 4+ gols.
        - 'DECISAO': Final ou Semifinal.
        - 'BATALHA': Jogo com 5+ cartões no total.
        - 'JOGO_QUENTE': Outros casos interessantes.

        SAÍDA JSON:
        { "titulo": "Manchete curta, impactante e engraçada (com emoji)", "mensagem": "Resumo jornalístico sensacionalista e divertido (max 200 chars)", "tipo": "..." }
        """,
                torneio, nomeFase,
                dadosMandante,
                dadosVisitante,
                placar, "", cAmarelos, cVermelhos, contextoPartida);
    }

    private String montarContextoPartida(Partida p) {
        StringBuilder sb = new StringBuilder();

        switch (p.getTipoPartida()) {
            case MATA_MATA_IDA -> sb.append("- Jogo de IDA de um confronto em dois jogos (mata-mata). O placar desta partida NÃO define o classificado; a decisão será no jogo de volta.\n");
            case MATA_MATA_VOLTA -> {
                sb.append("- Jogo de VOLTA de um confronto em dois jogos (mata-mata).\n");
                sb.append(montarAgregado(p));
            }
            case FINAL_IDA -> sb.append("- Jogo de IDA da grande final (final em dois jogos). NÃO define o campeão ainda.\n");
            case FINAL_VOLTA -> {
                sb.append("- Jogo de VOLTA da grande final (final em dois jogos).\n");
                sb.append(montarAgregado(p));
            }
            case MATA_MATA_UNICO -> sb.append("- Jogo único e eliminatório: esta partida decide o classificado.\n");
            case FINAL_UNICA -> sb.append("- Final em jogo único: esta partida decide o campeão.\n");
            case DISPUTA_TERCEIRO_LUGAR -> sb.append("- Disputa pelo terceiro lugar do torneio.\n");
            case FASE_DE_GRUPOS, PONTOS_CORRIDOS -> sb.append("- Jogo de fase de grupos/pontos corridos, não é mata-mata.\n");
        }

        if (p.isHouveProrrogacao()) {
            sb.append("- O resultado foi definido na PRORROGAÇÃO.\n");
        }

        if (p.houvePenaltis()) {
            sb.append(String.format("- Houve DISPUTA DE PÊNALTIS, decidida em %d x %d.\n",
                    p.getPenaltis().getGolsMandante(), p.getPenaltis().getGolsVisitante()));
        }

        if (p.isWo()) {
            sb.append("- A partida foi decidida por W.O. (walkover), não houve jogo de fato.\n");
        }

        return sb.toString();
    }

    private String montarAgregado(Partida volta) {
        TipoPartida tipoIda = (volta.getTipoPartida() == TipoPartida.MATA_MATA_VOLTA)
                ? TipoPartida.MATA_MATA_IDA
                : TipoPartida.FINAL_IDA;

        List<PlacarIdaDTO> resultados = partidaRepository.buscarPlacarIda(
                volta.getFase().getId(),
                volta.getChaveIndex(),
                tipoIda,
                volta.getVisitante().getId(),
                volta.getMandante().getId());

        if (resultados.isEmpty() || !resultados.get(0).realizada()) {
            return "- Não há registro do jogo de ida (ou ele ainda não foi realizado); trate apenas o placar desta partida.\n";
        }

        PlacarIdaDTO ida = resultados.get(0);

        int totalMandanteVolta = nz(volta.getGolsMandante()) + nz(ida.golsVisitante());
        int totalVisitanteVolta = nz(volta.getGolsVisitante()) + nz(ida.golsMandante());

        return String.format(
                "- Placar do jogo de ida: %d x %d. Placar agregado (ida + volta): %s %d x %d %s.\n",
                ida.golsMandante(), ida.golsVisitante(),
                volta.getMandante().getJogador().getNome(), totalMandanteVolta,
                totalVisitanteVolta, volta.getVisitante().getJogador().getNome());
    }

    private int nz(Integer v) {
        return v != null ? v : 0;
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