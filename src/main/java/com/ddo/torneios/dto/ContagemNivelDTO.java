package com.ddo.torneios.dto;

import com.ddo.torneios.model.NivelAdvertencia;

public record ContagemNivelDTO(NivelAdvertencia nivel, int ativas, int limiteConversao) {
}
