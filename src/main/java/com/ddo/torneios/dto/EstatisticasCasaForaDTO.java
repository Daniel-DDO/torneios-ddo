package com.ddo.torneios.dto;

public record EstatisticasCasaForaDTO(
        String jogadorId,
        String nome,
        String discord,
        String imagem,

        //Como mandante nos jogos por clube
        long vClubeCasa, long eClubeCasa, long dClubeCasa,
        //Como mandante nos jogos pela seleção
        long vSelecaoCasa, long eSelecaoCasa, long dSelecaoCasa,

        //Como visitante nos jogos por clube
        long vClubeFora, long eClubeFora, long dClubeFora,
        //Como visitante nos jogos pela seleção
        long vSelecaoFora, long eSelecaoFora, long dSelecaoFora
) {
    public long totalCasa() { return vClubeCasa+eClubeCasa+dClubeCasa+vSelecaoCasa+eSelecaoCasa+dSelecaoCasa; }
    public long totalFora() { return vClubeFora+eClubeFora+dClubeFora+vSelecaoFora+eSelecaoFora+dSelecaoFora; }

    public String aproveitamentoCasa() {
        long v = vClubeCasa+vSelecaoCasa, e = eClubeCasa+eSelecaoCasa, t = totalCasa();
        return t == 0 ? "0.0%" : String.format("%.1f%%", ((v*3.0+e)/(t*3.0))*100.0);
    }

    public String aproveitamentoFora() {
        long v = vClubeFora+vSelecaoFora, e = eClubeFora+eSelecaoFora, t = totalFora();
        return t == 0 ? "0.0%" : String.format("%.1f%%", ((v*3.0+e)/(t*3.0))*100.0);
    }

    public boolean temJogosEmCasa() { return totalCasa() > 0; }
    public boolean temJogosFora() { return totalFora() > 0; }
}