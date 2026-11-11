package com.ddo.torneios.event;

import com.ddo.torneios.model.OrigemSuspensaoAdvertencia;
import com.ddo.torneios.model.TipoSuspensaoAdvertencia;

import java.util.List;

public record SuspensaoPorAdvertenciaGeradaEvent(
        String suspensaoId,
        String jogadorId,
        TipoSuspensaoAdvertencia tipo,
        int quantidade,
        OrigemSuspensaoAdvertencia origem,
        List<String> advertenciaIds
) {
}
