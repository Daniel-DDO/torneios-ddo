package com.ddo.torneios.dto;

public record AgregadoCasaForaDTO(
        Long vClubeCasa, Long eClubeCasa, Long dClubeCasa,
        Long vSelecaoCasa, Long eSelecaoCasa, Long dSelecaoCasa,
        Long vClubeFora, Long eClubeFora, Long dClubeFora,
        Long vSelecaoFora, Long eSelecaoFora, Long dSelecaoFora
) {}