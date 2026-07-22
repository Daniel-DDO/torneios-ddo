package com.ddo.torneios.request;

import lombok.Data;
import java.util.List;

@Data
public class ConfirmacaoSorteioRequest {
    private String temporadaId;
    private List<ParInscricao> inscricoes;

    @Data
    public static class ParInscricao {
        private String jogadorId;
        private String clubeId;
    }
}