package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;

public record TrocarCapitaoRequest(@NotBlank String novoCapitaoId) {}
