package com.ddo.torneios.dto;

import java.util.List;

public record EquipeTabelaoDTO(ClassificacaoEquipeDTO classificacao, List<MembroEquipeDTO> membros) {}