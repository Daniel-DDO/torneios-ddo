package com.ddo.torneios.dto;

import java.time.LocalDateTime;

public record NotificacaoLanceDTO(
        String nomeJogador,
        LocalDateTime horario
) {}