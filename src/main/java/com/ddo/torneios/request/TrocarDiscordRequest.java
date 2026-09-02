package com.ddo.torneios.request;

import jakarta.validation.constraints.NotBlank;

public record TrocarDiscordRequest(
        @NotBlank String novoDiscord
) {}