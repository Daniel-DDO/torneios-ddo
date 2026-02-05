package com.ddo.torneios.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IniciarLeilaoRequest(
        @NotNull String temporadaId,
        @NotNull @Positive Integer horasDuracao,
        boolean isSelecao
) {}