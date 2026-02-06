package com.ddo.torneios.request;

import com.ddo.torneios.model.TipoNotificacao;

public record NotificacaoRequest(
        String jogadorId,
        String titulo,
        String mensagem,
        String link,
        TipoNotificacao tipo
) {}