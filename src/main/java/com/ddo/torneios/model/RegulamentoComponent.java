package com.ddo.torneios.model;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RegulamentoComponent {

    @Value("classpath:regulamento_ddo.txt")
    private Resource regulamentoResource;

    @Getter
    private String textoRegulamento;

    @PostConstruct
    public void init() {
        try {
            this.textoRegulamento = StreamUtils.copyToString(
                    regulamentoResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            log.info("Regulamento carregado na memória compartilhada (Tamanho: {} chars)", textoRegulamento.length());
        } catch (Exception e) {
            log.error("Erro crítico ao carregar regulamento compartilhado", e);
            this.textoRegulamento = "ERRO: Regulamento indisponível.";
        }
    }

    private static final int LIMITE_CARACTERES_FALLBACK = 20000;

    public String getTextoRegulamentoParaFallback() {
        String texto = getTextoRegulamento();
        if (texto.length() <= LIMITE_CARACTERES_FALLBACK) {
            return texto;
        }
        return texto.substring(0, LIMITE_CARACTERES_FALLBACK)
                + "\n\n[Regulamento truncado por limite de contexto. Se a resposta não estiver clara aqui, "
                + "oriente o jogador a consultar um administrador ou o regulamento completo no site.]";
    }
}