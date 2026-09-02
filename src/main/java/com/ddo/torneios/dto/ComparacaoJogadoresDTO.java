package com.ddo.torneios.dto;

import java.util.List;

public record ComparacaoJogadoresDTO(
        DadosJogadorComparacao jogador1,
        DadosJogadorComparacao jogador2,
        List<PartidaHistoricoDTO> confrontosDiretos,
        ResumoConfrontoDiretoDTO resumoConfrontoDireto,
        AnaliseComparativaDTO analiseComparativa
) {
    public record DadosJogadorComparacao(
            String id, String nome, String discord, String imagem,
            int titulos, int finais, int jogos, int vitorias,
            int golsMarcados, int golsSofridos, String aproveitamento,
            java.math.BigDecimal saldoVirtual, java.math.BigDecimal pontosCoeficiente,
            EstatisticasCasaForaDTO casaFora,
            EstiloJogadorDTO estilo,
            FormaRecenteDTO formaRecente
    ) {}

    public record FormaRecenteDTO(
            List<String> ultimosResultados, // ["V","V","E","D","V"]
            int pontuacaoForma,             // 0 a 15
            String tendencia                // "Em ascensão" / "Estável" / "Em queda"
    ) {}

    public record AnaliseComparativaDTO(
            double vantagemMandoJogador1,   // aproveitamento casa - fora
            double vantagemMandoJogador2,
            String favoritoGeral,           // nome de quem tem a vantagem estimada
            double margemVantagem,          // 0 a 100, "confiança" da estimativa
            String leituraEstilistica,      // texto explicando o cruzamento de estilos
            List<String> pontosDeAtencao    // insights extras (ex: "j1 depende muito de mando")
    ) {}
}