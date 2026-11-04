package com.ddo.torneios.request;

import jakarta.validation.constraints.NotNull;

public record AtualizarClubeInscricaoRequest(@NotNull String clubeId) {}
