package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.TipoPartida;
import com.ddo.torneios.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProbabilidadeService {

    @Autowired
    private PartidaRepository partidaRepository;

    private static final double PESO_CLUBE = 0.35;
    private static final double PESO_MOMENTO = 0.25;
    private static final double PESO_HISTORICO = 0.20;
    private static final double PESO_CONFRONTO = 0.20;

    private static final double FATOR_CASA = 1.05;
    private static final double DESVIO_EMPATE = 19.0;

    private static final double PESO_ATAQUE = 0.55;
    private static final double PESO_DEFESA_ADVERSARIO = 0.45;
    private static final double MEDIA_GOLS_LIGA = 2.6;
    private static final double FATOR_CASA_GOLS = 1.10;

    public ProbabilidadePartidaDTO calcularProbabilidade(PartidaProbabilidadeDTO partida) {
        if (partida.realizada()) {
            return new ProbabilidadePartidaDTO(0, 0, 0, "Partida finalizada.", null);
        }
        if (partida.mandanteJogadorId() == null || partida.visitanteJogadorId() == null) {
            return new ProbabilidadePartidaDTO(50, 0, 50, "Aguardando oponentes.", null);
        }

        HistoricoConfrontoProjection h2h = partidaRepository.findResumoConfrontoDireto(
                partida.mandanteJogadorId(),
                partida.visitanteJogadorId()
        );

        double scoreM = calcularScoreTotal(
                partida.mandanteClubeEstrelas(), partida.mandantePartidasJogadas(), partida.mandanteVitorias(),
                partida.mandantePartidasJogadasNaTemporada(), partida.mandanteAproveitamentoTemporada(),
                h2h, true
        );
        double scoreV = calcularScoreTotal(
                partida.visitanteClubeEstrelas(), partida.visitantePartidasJogadas(), partida.visitanteVitorias(),
                partida.visitantePartidasJogadasNaTemporada(), partida.visitanteAproveitamentoTemporada(),
                h2h, false
        );

        scoreM *= FATOR_CASA;

        ContextoAnalise contexto = new ContextoAnalise();
        analisarTabu(h2h, partida.mandanteJogadorNome(), partida.visitanteJogadorNome(), contexto);

        TipoPartida tipo = partida.tipoPartida();

        if (tipo == TipoPartida.MATA_MATA_VOLTA || tipo == TipoPartida.FINAL_VOLTA) {
            ResultadoIda resultadoIda = analisarJogoIda(partida.id());
            if (resultadoIda.temJogoAnterior()) {
                aplicarLogicaJogoVolta(resultadoIda, contexto);
            }
        } else if (tipo == TipoPartida.MATA_MATA_UNICO || tipo == TipoPartida.FINAL_UNICA) {
            contexto.mensagens.add("Decisão única (Chance de pênaltis considerada).");
            contexto.ajusteEmpate += 5.0;
        }

        double delta = scoreM - scoreV;

        double baseEmpate = 30.0 + contexto.ajusteEmpate;
        double probEmpate = baseEmpate * Math.exp(-Math.pow(delta, 2) / (2 * Math.pow(DESVIO_EMPATE, 2)));
        probEmpate = Math.max(5, Math.min(65, probEmpate));

        double restante = 100.0 - probEmpate;
        double sigmoide = 1.0 / (1.0 + Math.pow(10, -delta / 35.0));
        sigmoide += (contexto.ajusteMandante / 100.0);

        double probMandante = restante * sigmoide;
        double probVisitante = restante * (1.0 - sigmoide);

        String analiseFinal = contexto.mensagens.isEmpty() ? "Confronto equilibrado." : String.join(" ", contexto.mensagens);

        if (Math.abs(probMandante - probVisitante) < 3 && probEmpate > 25) analiseFinal += " Previsão de duelo truncado.";
        if (probMandante > 70) analiseFinal = "Favoritismo absoluto do mandante. " + analiseFinal;
        if (probVisitante > 60) analiseFinal = "Visitante chega muito forte. " + analiseFinal;

        MediasGolsCasaForaDTO mediasCasaFora = partidaRepository.buscarMediasGolsCasaFora(
                partida.mandanteJogadorId(), partida.visitanteJogadorId()
        );

        ProbabilidadePartidaDTO.PlacarCotadoDTO placar = calcularPlacarCotado(partida, mediasCasaFora);

        return new ProbabilidadePartidaDTO(
                (int) Math.round(probMandante),
                (int) Math.round(probEmpate),
                (int) Math.round(probVisitante),
                analiseFinal.trim(),
                placar
        );
    }

    private double calcularScoreTotal(BigDecimal clubeEstrelas, Integer partidasJogadas, Integer vitorias,
                                      Integer partidasTemporada, Double aproveitamentoTemporada,
                                      HistoricoConfrontoProjection h2h, boolean isMandanteNoH2H) {

        double estrelas = clubeEstrelas != null ? clubeEstrelas.doubleValue() : 3.0;
        double sClube = estrelas * 20.0;

        int jogos = nz(partidasJogadas);
        double winRate = jogos > 0 ? (double) nz(vitorias) / jogos : 0.5;
        double sHistorico = winRate * 100.0;

        double sMomento = sHistorico;
        if (partidasTemporada != null && partidasTemporada >= 3) {
            sMomento = aproveitamentoTemporada != null ? aproveitamentoTemporada : 50.0;
        }

        double sH2H = 50.0;
        if (h2h != null && h2h.getTotalJogos() > 0) {
            int vitoriasMinhas = isMandanteNoH2H ? h2h.getVitoriasMandanteAtual() : h2h.getVitoriasVisitanteAtual();
            double aproveitamentoH2H = (double) (vitoriasMinhas + (h2h.getEmpates() * 0.5)) / h2h.getTotalJogos();
            sH2H = aproveitamentoH2H * 100.0;
        }

        return (sClube * PESO_CLUBE) +
                (sMomento * PESO_MOMENTO) +
                (sHistorico * PESO_HISTORICO) +
                (sH2H * PESO_CONFRONTO);
    }

    private void analisarTabu(HistoricoConfrontoProjection h2h, String nomeM, String nomeV, ContextoAnalise ctx) {
        if (h2h == null || h2h.getTotalJogos() < 2) return;

        int diff = h2h.getVitoriasMandanteAtual() - h2h.getVitoriasVisitanteAtual();

        if (diff >= 3) {
            ctx.mensagens.add(nomeM + " tem ampla paternidade histórica sobre " + nomeV + ".");
        } else if (diff <= -3) {
            ctx.mensagens.add(nomeV + " costuma levar a melhor nos confrontos diretos.");
        } else if (h2h.getEmpates() > (h2h.getTotalJogos() * 0.6)) {
            ctx.mensagens.add("Histórico de muitos empates entre os dois.");
        }
    }

    private void aplicarLogicaJogoVolta(ResultadoIda resultado, ContextoAnalise ctx) {
        int saldo = resultado.saldoMandanteAtual();
        int golsMandante = resultado.golsIdaMandanteAtual();
        int golsVisitante = resultado.golsIdaVisitanteAtual();
        int totalGolsIda = golsMandante + golsVisitante;

        String placarIda = golsVisitante + "x" + golsMandante; // na perspectiva de quem era mandante NA IDA

        if (saldo <= -4) {
            ctx.mensagens.add("Goleada sofrida na ida (" + placarIda + "). Mandante praticamente precisa de um milagre para reverter.");
            ctx.ajusteEmpate -= 20.0;
            ctx.ajusteMandante -= 10.0;
        } else if (saldo == -3) {
            ctx.mensagens.add("Ida foi uma goleada (" + placarIda + "). Virada é improvável, mas não impossível.");
            ctx.ajusteEmpate -= 15.0;
            ctx.ajusteMandante -= 5.0;
        } else if (saldo == -2) {
            ctx.mensagens.add("Mandante perdeu por dois gols de diferença na ida (" + placarIda + "). Precisa de uma boa atuação para reverter.");
            ctx.ajusteEmpate -= 10.0;
        } else if (saldo == -1) {
            if (totalGolsIda >= 5) {
                ctx.mensagens.add("Ida movimentada e apertada (" + placarIda + "). Mandante saiu atrás por pouco, mas o confronto foi aberto.");
            } else {
                ctx.mensagens.add("Mandante pressionado pela vitória após derrota mínima na ida (" + placarIda + ").");
            }
            ctx.ajusteEmpate -= 5.0;
            ctx.ajusteMandante += 5.0;
        } else if (saldo == 0) {
            if (totalGolsIda == 0) {
                ctx.mensagens.add("Ida travada e sem gols (0x0). Confronto decidido praticamente do zero na volta.");
            } else {
                ctx.mensagens.add("Ida terminou empatada (" + placarIda + "). Tudo em aberto para a decisão.");
            }
            ctx.mensagens.add("Confronto totalmente aberto.");
        } else if (saldo == 1) {
            ctx.mensagens.add("Mandante vem de vitória apertada na ida (" + placarIda + "). Vantagem mínima, nada garantido ainda.");
            ctx.ajusteMandante += 3.0;
        } else if (saldo == 2) {
            ctx.mensagens.add("Mandante confortável com a vantagem construída na ida (" + placarIda + ").");
            ctx.ajusteEmpate += 15.0;
            ctx.ajusteMandante -= 5.0;
        } else if (saldo == 3) {
            ctx.mensagens.add("Ida foi uma goleada a favor do mandante (" + placarIda + "). Vantagem muito confortável.");
            ctx.ajusteEmpate += 20.0;
            ctx.ajusteMandante -= 8.0;
        } else { // saldo >= 4
            ctx.mensagens.add("Goleada aplicada na ida (" + placarIda + "). Classificação praticamente encaminhada.");
            ctx.ajusteEmpate += 25.0;
            ctx.ajusteMandante -= 12.0;
        }
    }

    private ResultadoIda analisarJogoIda(String partidaVoltaId) {
        Optional<PartidaIdaResultadoDTO> idaOpt = partidaRepository.buscarResultadoPartidaIdaPorProximaPartida(partidaVoltaId);

        if (idaOpt.isEmpty()) {
            return new ResultadoIda(false, 0, 0, 0);
        }

        PartidaIdaResultadoDTO ida = idaOpt.get();

        // Na ida, o mandante de HOJE era o visitante (mandante e visitante se invertem entre ida e volta).
        // Então os gols do mandante de hoje na ida = golsVisitante da ida; e vice-versa.
        int golsMandanteAtualNaIda = nz(ida.golsVisitante());
        int golsVisitanteAtualNaIda = nz(ida.golsMandante());
        int saldo = golsMandanteAtualNaIda - golsVisitanteAtualNaIda;

        return new ResultadoIda(true, saldo, golsMandanteAtualNaIda, golsVisitanteAtualNaIda);
    }

    private static final int MAX_GOLS_MATRIZ = 6;

    private ProbabilidadePartidaDTO.PlacarCotadoDTO calcularPlacarCotado(
            PartidaProbabilidadeDTO partida, MediasGolsCasaForaDTO mediasCasaFora) {

        double expectativaMandante = calcularExpectativaMandante(partida, mediasCasaFora);
        double expectativaVisitante = calcularExpectativaVisitante(partida, mediasCasaFora);

        // Monta a matriz de probabilidades P(mandante marca X) x P(visitante marca Y)
        double[][] matriz = new double[MAX_GOLS_MATRIZ + 1][MAX_GOLS_MATRIZ + 1];
        double somaTotal = 0.0;

        for (int golsM = 0; golsM <= MAX_GOLS_MATRIZ; golsM++) {
            double probM = poissonPmf(expectativaMandante, golsM);
            for (int golsV = 0; golsV <= MAX_GOLS_MATRIZ; golsV++) {
                double probV = poissonPmf(expectativaVisitante, golsV);
                double probCombinada = probM * probV;
                matriz[golsM][golsV] = probCombinada;
                somaTotal += probCombinada;
            }
        }

        // Normaliza (a matriz truncada em 6 gols não soma exatamente 100%, ajusta pra fechar a conta)
        int melhorGolsM = 0, melhorGolsV = 0;
        double melhorProb = -1;
        double probAmbosMarcam = 0.0;
        double probMaisDe2Meio = 0.0;

        List<ProbabilidadePartidaDTO.PlacarProvavelDTO> candidatos = new ArrayList<>();

        for (int golsM = 0; golsM <= MAX_GOLS_MATRIZ; golsM++) {
            for (int golsV = 0; golsV <= MAX_GOLS_MATRIZ; golsV++) {
                double probNormalizada = matriz[golsM][golsV] / somaTotal;

                if (probNormalizada > melhorProb) {
                    melhorProb = probNormalizada;
                    melhorGolsM = golsM;
                    melhorGolsV = golsV;
                }
                if (golsM >= 1 && golsV >= 1) {
                    probAmbosMarcam += probNormalizada;
                }
                if (golsM + golsV >= 3) {
                    probMaisDe2Meio += probNormalizada;
                }
                candidatos.add(new ProbabilidadePartidaDTO.PlacarProvavelDTO(golsM, golsV, round2(probNormalizada * 100)));
            }
        }

        List<ProbabilidadePartidaDTO.PlacarProvavelDTO> top3 = candidatos.stream()
                .sorted((a, b) -> Double.compare(b.probabilidade(), a.probabilidade()))
                .limit(3)
                .toList();

        String observacao = String.format(
                "Placar cotado (provável) via distribuição de Poisson sobre médias de gols específicas de mando de campo. "
                        + "Expectativa: %.2f x %.2f gols. Chance de ambos marcarem: %.1f%%.",
                expectativaMandante, expectativaVisitante, round2(probAmbosMarcam * 100)
        );

        return new ProbabilidadePartidaDTO.PlacarCotadoDTO(
                melhorGolsM, melhorGolsV, round2(melhorProb * 100),
                round2(expectativaMandante), round2(expectativaVisitante),
                round2(probAmbosMarcam * 100), round2(probMaisDe2Meio * 100),
                top3, observacao
        );
    }

    private double calcularExpectativaMandante(PartidaProbabilidadeDTO partida, MediasGolsCasaForaDTO m) {
        double ataqueCasa = m.mandanteMediaGolsMarcadosCasa() != null
                ? m.mandanteMediaGolsMarcadosCasa()
                : mediaGolsPorJogo(partida.mandanteGolsMarcadosTemporada(), partida.mandantePartidasJogadasNaTemporada(),
                partida.mandanteGolsMarcados(), partida.mandantePartidasJogadas());

        double defesaForaAdversario = m.visitanteMediaGolsSofridosFora() != null
                ? m.visitanteMediaGolsSofridosFora()
                : mediaGolsPorJogo(partida.visitanteGolsSofridosTemporada(), partida.visitantePartidasJogadasNaTemporada(),
                partida.visitanteGolsSofridos(), partida.visitantePartidasJogadas());

        return Math.max(0.1, (ataqueCasa * PESO_ATAQUE) + (defesaForaAdversario * PESO_DEFESA_ADVERSARIO)) * FATOR_CASA_GOLS;
    }

    private double calcularExpectativaVisitante(PartidaProbabilidadeDTO partida, MediasGolsCasaForaDTO m) {
        double ataqueFora = m.visitanteMediaGolsMarcadosFora() != null
                ? m.visitanteMediaGolsMarcadosFora()
                : mediaGolsPorJogo(partida.visitanteGolsMarcadosTemporada(), partida.visitantePartidasJogadasNaTemporada(),
                partida.visitanteGolsMarcados(), partida.visitantePartidasJogadas());

        double defesaCasaAdversario = m.mandanteMediaGolsSofridosCasa() != null
                ? m.mandanteMediaGolsSofridosCasa()
                : mediaGolsPorJogo(partida.mandanteGolsSofridosTemporada(), partida.mandantePartidasJogadasNaTemporada(),
                partida.mandanteGolsSofridos(), partida.mandantePartidasJogadas());

        return Math.max(0.1, (ataqueFora * PESO_ATAQUE) + (defesaCasaAdversario * PESO_DEFESA_ADVERSARIO));
    }

    private double poissonPmf(double lambda, int k) {
        return Math.pow(lambda, k) * Math.exp(-lambda) / fatorial(k);
    }

    private double fatorial(int n) {
        double resultado = 1.0;
        for (int i = 2; i <= n; i++) resultado *= i;
        return resultado;
    }

    private double mediaGolsPorJogo(Integer golsTemporada, Integer jogosTemporada, Integer golsCarreira, Integer jogosCarreira) {
        if (jogosTemporada != null && jogosTemporada >= 3 && golsTemporada != null) {
            return golsTemporada / (double) jogosTemporada;
        }
        if (jogosCarreira != null && jogosCarreira > 0 && golsCarreira != null) {
            return golsCarreira / (double) jogosCarreira;
        }
        return MEDIA_GOLS_LIGA / 2.0;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class ContextoAnalise {
        List<String> mensagens = new ArrayList<>();
        double ajusteEmpate = 0.0;
        double ajusteMandante = 0.0;
    }

    private record ResultadoIda(boolean temJogoAnterior, int saldoMandanteAtual, int golsIdaMandanteAtual, int golsIdaVisitanteAtual) {}
}