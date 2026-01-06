package com.ddo.torneios.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Clube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String nome;

    @NotBlank
    private String estadio;

    @Column(columnDefinition = "TEXT")
    private String imagem;

    @Enumerated(EnumType.STRING)
    private LigaClube ligaClube;

    @NotBlank
    @Size(min = 3, max = 3, message = "A sigla deve ter exatamente 3 letras")
    @Column(length = 3, unique = true)
    private String sigla;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Formato de cor inválido")
    private String corPrimaria;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Formato de cor inválido")
    private String corSecundaria;

    @ColumnDefault("true")
    private boolean ativo = true;

    private BigDecimal estrelas;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer titulos = 0;

    private BigDecimal valorAvaliado;

    public Clube(String nome, String estadio, String imagem, LigaClube ligaClube, String sigla, String corPrimaria, String corSecundaria, BigDecimal estrelas) {
        this.nome = nome;
        this.estadio = estadio;
        this.imagem = imagem;
        this.ligaClube = ligaClube;
        this.sigla = sigla;
        this.corPrimaria = corPrimaria;
        this.corSecundaria = corSecundaria;
        this.estrelas = estrelas;
    }
}
