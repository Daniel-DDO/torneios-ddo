package com.ddo.torneios.service;

import com.ddo.torneios.model.Noticia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DiscordNotificationService {

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void enviarNoticia(Noticia noticia) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook do Discord não configurado!");
            return;
        }

        try {
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", noticia.getTitulo());
            embed.put("description", noticia.getMensagem());
            embed.put("url", noticia.getLinkPartida());
            embed.put("color", getColorByTipo(noticia.getTipo()));

            Map<String, String> footer = new HashMap<>();
            footer.put("text", "Torneios DDO • IA Reporter");
            embed.put("footer", footer);

            embed.put("timestamp", noticia.getDataCriacao().toString());

            Map<String, Object> payload = new HashMap<>();
            payload.put("username", "DDO News");
            payload.put("avatar_url", "https://cdn-icons-png.flaticon.com/512/2585/2585184.png"); // Ícone de Jornal
            payload.put("embeds", List.of(embed));

            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("Notificação enviada para o Discord: {}", noticia.getTitulo());

        } catch (Exception e) {
            log.error("Erro ao enviar para Discord: ", e);
        }
    }

    private int getColorByTipo(String tipo) {
        if (tipo == null) return 3447003;

        return switch (tipo.toUpperCase()) {
            case "TITANS" -> 16776960;
            case "ZEBRA" -> 10181046;
            case "GOLEADA" -> 15158332;
            case "DECISAO" -> 3066993;
            case "BATALHA" -> 15105570;
            default -> 3447003;
        };
    }
}