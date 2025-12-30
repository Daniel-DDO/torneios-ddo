package com.ddo.torneios.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaRequest {
    private String nome;
    private String descricao;
    private String imagem;
}
