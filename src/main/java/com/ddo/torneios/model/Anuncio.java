package com.ddo.torneios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Anuncio {

    @Id
    private String id;

    @NotBlank
    private String titulo;

    private String mensagem;
    private String tipoMensagem;
    private LocalDateTime dataPostagem;
    private String corMensagem;
}
