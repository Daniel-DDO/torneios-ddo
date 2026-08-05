package com.ddo.torneios.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Contador(AtomicInteger tentativas, long inicioJanela) {}

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

    private static final Map<String, Integer> LIMITES_ESPECIFICOS = Map.of(
            "/jogador/login", 5,
            "/jogador/reivindicar", 3,
            "/jogador/reivindicar-direto", 3,
            "/jogador/recuperar-senha", 3,
            "/jogador/gerar-codigo", 5
    );

    private static final int LIMITE_PADRAO = 60;
    private static final long JANELA_MS = 60_000; // 1 minuto

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = extrairIp(request);
        String path = request.getRequestURI();
        int limite = LIMITES_ESPECIFICOS.getOrDefault(path, LIMITE_PADRAO);
        String chave = ip + ":" + path;

        long agora = Instant.now().toEpochMilli();

        Contador contador = contadores.compute(chave, (k, atual) -> {
            if (atual == null || (agora - atual.inicioJanela()) > JANELA_MS) {
                return new Contador(new AtomicInteger(1), agora);
            }
            atual.tentativas().incrementAndGet();
            return atual;
        });

        if (contador.tentativas().get() > limite) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"Muitas requisições. Tente novamente em instantes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}