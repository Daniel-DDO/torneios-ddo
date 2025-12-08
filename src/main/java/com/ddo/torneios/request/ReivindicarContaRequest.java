package com.ddo.torneios.request;

import lombok.Data;

@Data
public class ReivindicarContaRequest {
    private String discord;
    private String codigo;
    private String novoEmail;
    private String novaSenha;
}
