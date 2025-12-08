package com.ddo.torneios.request;

import lombok.Data;

@Data
public class RecuperarSenhaRequest {
    private String discord;
    private Integer pin;
    private String novaSenha;
}
