package com.ddo.torneios.request;

import com.ddo.torneios.model.StatusJogador;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtualizarStatusRequest {
    @NotNull(message = "O status não pode ser nulo")
    private StatusJogador status;
}