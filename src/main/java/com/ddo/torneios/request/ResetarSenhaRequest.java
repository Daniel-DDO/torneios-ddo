package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetarSenhaRequest(
        @NotBlank @Size(min = 6) String novaSenha
) {}