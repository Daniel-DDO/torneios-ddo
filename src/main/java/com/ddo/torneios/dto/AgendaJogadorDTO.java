package com.ddo.torneios.dto;

import java.util.List;

public record AgendaJogadorDTO(String identificador, List<PartidaPdfDTO> partidas) {}