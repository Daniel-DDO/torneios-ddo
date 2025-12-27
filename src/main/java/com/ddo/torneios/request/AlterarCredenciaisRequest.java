package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlterarCredenciaisRequest {

    @NotBlank(message = "A senha atual é obrigatória para realizar alterações")
    private String senhaAtual;

    private String novoEmail;

    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    private String novaSenha;
}