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
}