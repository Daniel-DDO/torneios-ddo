package com.ddo.torneios.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AnuncioRequest {
    private String titulo;
    private String mensagem;
    private String tipoMensagem;
    private String imagem;
    private LocalDateTime dataPostagem;
    private String corMensagem;
}
