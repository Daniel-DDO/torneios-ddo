package com.ddo.torneios.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RealizarLanceDTO(
        @NotNull String leilaoId,
        @NotNull List<ItemLanceDTO> preferencias
) {}