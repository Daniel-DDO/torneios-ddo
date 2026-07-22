package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record AnuncioDTO (
        String id,
        String titulo,
        String mensagem,
        LocalDateTime dataPostagem,
        String tipoMensagem,
        String imagem,
        String corMensagem
) {}