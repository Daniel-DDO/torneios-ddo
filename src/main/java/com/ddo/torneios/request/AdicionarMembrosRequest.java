package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdicionarMembrosRequest(@NotBlank String faseId, @NotEmpty List<String> jogadorClubeIds) {}
