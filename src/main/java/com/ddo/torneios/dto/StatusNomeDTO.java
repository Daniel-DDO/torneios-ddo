package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record StatusNomeDTO(
        String jogadorId,
        String nome,
        String discord,
        String imagem,
        boolean negativado,
        boolean jaFoiNegativadoAlgumaVez,
        LocalDateTime dataNegativacao,
        LocalDateTime dataQuitacaoNegativacao
) {}