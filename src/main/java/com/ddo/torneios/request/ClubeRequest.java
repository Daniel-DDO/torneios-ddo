package com.ddo.torneios.request;

import com.ddo.torneios.model.LigaClube;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClubeRequest {

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "A sigla é obrigatória")
    @Size(min = 3, max = 3, message = "A sigla deve ter exatamente 3 letras")
    private String sigla;

    @NotBlank(message = "O estádio é obrigatório")
    private String estadio;

    private String imagem;

    @NotNull(message = "A liga é obrigatória")
    private LigaClube ligaClube;

    @DecimalMin(value = "0.5", message = "O mínimo é 0.5 estrelas")
    @DecimalMax(value = "5.0", message = "O máximo é 5.0 estrelas")
    private BigDecimal estrelas;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor primária inválida")
    private String corPrimaria;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor secundária inválida")
    private String corSecundaria;
}