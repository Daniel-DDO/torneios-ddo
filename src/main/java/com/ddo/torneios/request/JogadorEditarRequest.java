package com.ddo.torneios.request;

import lombok.Data;

@Data
public class JogadorEditarRequest {
    private String nome;
    private String imagem;
    private String descricao;
}
