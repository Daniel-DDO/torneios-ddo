package com.ddo.torneios.dto;

import com.ddo.torneios.model.Notificacao;
import com.ddo.torneios.model.TipoNotificacao;
import java.time.LocalDateTime;

public record NotificacaoDTO(
        String id,
        String titulo,
        String mensagem,
        String link,
        TipoNotificacao tipo,
        boolean lida,
        LocalDateTime dataCriacao
) {
    public static NotificacaoDTO fromEntity(Notificacao n) {
        return new NotificacaoDTO(
                n.getId(),
                n.getTitulo(),
                n.getMensagem(),
                n.getLink(),
                n.getTipo(),
                n.isLida(),
                n.getDataCriacao()
        );
    }

    public NotificacaoDTO(String titulo, String mensagem, String link) {
        this(null, titulo, mensagem, link, TipoNotificacao.INFORMACAO, false, LocalDateTime.now());
    }
}