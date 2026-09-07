package com.ddo.torneios.dto;

public record EstatisticasCasaForaDTO(
        String jogadorId, String nome, String discord, String imagem,

        long vClubeCasa, long eClubeCasa, long dClubeCasa,
        long vSelecaoCasa, long eSelecaoCasa, long dSelecaoCasa,

        long vClubeFora, long eClubeFora, long dClubeFora,
        long vSelecaoFora, long eSelecaoFora, long dSelecaoFora,

        long golsMarcadosCasa, long golsSofridosCasa,
        long golsMarcadosFora, long golsSofridosFora
) {
    public long saldoGolsCasa() { return golsMarcadosCasa - golsSofridosCasa; }
    public long saldoGolsFora() { return golsMarcadosFora - golsSofridosFora; }

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