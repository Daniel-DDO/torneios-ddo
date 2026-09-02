package com.ddo.torneios.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AtualizarEmailAdminRequest(
        @NotBlank @Email String novoEmail
) {}