package com.ddo.torneios.dto;

import java.util.List;

public record ChatRequestDTO(List<MensagemChatDTO> historico, String novaPergunta) {}