package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record ConquistaResumoDTO(
        String id,
        String tituloNome,
        String tituloImagem,
        String nomeEdicao,
        String jogadorNome,
        String clubeNome,
        String imagem,
        LocalDateTime dataConquista
) {}