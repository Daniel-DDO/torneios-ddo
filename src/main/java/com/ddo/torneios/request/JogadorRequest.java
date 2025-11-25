package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JogadorRequest {
    @NotBlank
    private String nome;

    @NotBlank
    private String discord;
}
