package com.ddo.torneios.dto;

public record CompeticaoDTO(
        String id,
        String nome,
        String imagem,
        String divisao,
        Integer valor,
        String descricao,
        String tituloId,
        String tituloNome,
        String tituloImagem
) {}