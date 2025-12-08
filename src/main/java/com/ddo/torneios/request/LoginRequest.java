package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String login;

    @NotBlank
    private String senha;
}
