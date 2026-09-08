package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
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

    private static final double FATOR_CASA_GOLS = 1.13;

    private static final double MEDIA_GOLS_LIGA = 5.57;

    private static final double PESO_ATAQUE = 0.55;
    private static final double PESO_DEFESA_ADVERSARIO = 0.45;

    private static final double SKEW_MAXIMO = 0.40;

    private static final int MAX_GOLS_MATRIZ = 6;

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

        ContextoAnalise contexto = new ContextoAnalise();
        analisarTabu(partida.mandanteJogadorId(), partida.visitanteJogadorId(), h2h,
                partida.mandanteJogadorNome(), partida.visitanteJogadorNome(), contexto);

        TipoPartida tipo = partida.tipoPartida();

        if (tipo == TipoPartida.MATA_MATA_VOLTA || tipo == TipoPartida.FINAL_VOLTA) {
            ResultadoIda resultadoIda = analisarJogoIda(
                    partida.id(), partida.mandanteJogadorId(), partida.visitanteJogadorId(), partida.etapaMataMata()
            );
            if (resultadoIda.temJogoAnterior()) {
                aplicarLogicaJogoVolta(resultadoIda, contexto);
            }
        } else if (tipo == TipoPartida.MATA_MATA_UNICO || tipo == TipoPartida.FINAL_UNICA) {
            contexto.mensagens.add("Decisão única (chance de pênaltis considerada em caso de empate).");
        }

        MediasGolsCasaForaDTO mediasCasaFora = partidaRepository.buscarMediasGolsCasaFora(
                partida.mandanteJogadorId(), partida.visitanteJogadorId()
        );

        double expectativaMandante = calcularExpectativaMandante(partida, mediasCasaFora);
        double expectativaVisitante = calcularExpectativaVisitante(partida, mediasCasaFora);

        // Unifica tudo numa única inclinação: score (clube + momento + histórico + H2H) + contexto de jogo de volta,
        // aplicada diretamente sobre a expectativa de gols — a mesma que gera o placar cotado.
        double deltaScore = (scoreM * 1.05) - scoreV; // 1.05 = pequena vantagem residual de mando já embutida no score
        double skew = clamp(deltaScore / 300.0, -SKEW_MAXIMO, SKEW_MAXIMO) + contexto.skewGols;
        skew = clamp(skew, -SKEW_MAXIMO * 1.5, SKEW_MAXIMO * 1.5);

        expectativaMandante = Math.max(0.1, expectativaMandante * (1 + skew));
        expectativaVisitante = Math.max(0.1, expectativaVisitante * (1 - skew));

        MatrizPoisson matriz = montarMatrizPoisson(expectativaMandante, expectativaVisitante);

        // Ajuste de contexto sobre o empate (ex: decisão única) — aplicado como reforço multiplicativo
        // na diagonal da matriz, com renormalização, em vez de uma fórmula gaussiana desacoplada.
        if (contexto.reforcoEmpate != 0) {
            matriz = aplicarReforcoEmpate(matriz, contexto.reforcoEmpate);
        }

        double probMandante = matriz.probVitoriaMandante() * 100.0;
        double probEmpate = matriz.probEmpate() * 100.0;
        double probVisitante = matriz.probVitoriaVisitante() * 100.0;

        String analiseFinal = contexto.mensagens.isEmpty()
                ? textoBaseSemContexto(probMandante, probVisitante)
                : String.join(" ", contexto.mensagens);

        if (Math.abs(probMandante - probVisitante) < 6 && probEmpate > 25) analiseFinal += " Previsão de duelo truncado.";
        if (probMandante > 70) analiseFinal = "Favoritismo absoluto do mandante. " + analiseFinal;
        if (probVisitante > 60) analiseFinal = "Visitante chega muito forte. " + analiseFinal;

        ProbabilidadePartidaDTO.PlacarCotadoDTO placar = extrairPlacarCotado(matriz, expectativaMandante, expectativaVisitante);

        return new ProbabilidadePartidaDTO(
                (int) Math.round(probMandante),
                (int) Math.round(probEmpate),
                (int) Math.round(probVisitante),
                analiseFinal.trim(),
                placar
        );
    }

    private String textoBaseSemContexto(double probMandante, double probVisitante) {
        double diff = Math.abs(probMandante - probVisitante);
        if (diff < 8) return "Confronto equilibrado.";
        return "Diferença técnica considerável entre os lados.";
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

    private void analisarTabu(String mandanteId, String visitanteId, HistoricoConfrontoProjection h2h,
                              String nomeM, String nomeV, ContextoAnalise ctx) {

        List<PartidaHistoricoResumoDTO> confrontos = partidaRepository
                .buscarTodosConfrontosDiretos(mandanteId, visitanteId);

        if (confrontos.isEmpty()) {
            // Sem histórico direto nenhum — o modelo já usa "momento" (forma recente/aproveitamento
            // na temporada) no cálculo do score, via calcularScoreTotal. Nada a fazer aqui além de
            // sinalizar isso no texto, pra deixar claro pro usuário por que a análise é mais "seca".
            ctx.mensagens.add("Sem confrontos diretos anteriores registrados — análise baseada no momento atual de cada jogador.");
            return;
        }

        // Peso decrescente por recência: o jogo mais recente pesa mais que os mais antigos.
        // Com N confrontos, os pesos vão de N (mais recente) até 1 (mais antigo).
        double somaSaldoPonderado = 0.0;
        double somaPesos = 0.0;
        int peso = confrontos.size();

        for (PartidaHistoricoResumoDTO jogo : confrontos) {
            boolean mandanteEraMandanteNesseJogo = jogo.mandanteJogadorId().equals(mandanteId);
            int golsMandanteAtual = mandanteEraMandanteNesseJogo ? nz(jogo.golsMandante()) : nz(jogo.golsVisitante());
            int golsVisitanteAtual = mandanteEraMandanteNesseJogo ? nz(jogo.golsVisitante()) : nz(jogo.golsMandante());
            int saldo = golsMandanteAtual - golsVisitanteAtual;

            somaSaldoPonderado += saldo * peso;
            somaPesos += peso;
            peso--;
        }

        double saldoMedioPonderado = somaSaldoPonderado / somaPesos;
        double skewHistorico = clamp(saldoMedioPonderado * 0.07, -SKEW_MAXIMO, SKEW_MAXIMO);
        ctx.skewGols += skewHistorico;

        // Mensagem textual: destaca o confronto mais recente se ele foi um resultado marcante
        PartidaHistoricoResumoDTO ultimo = confrontos.get(0);
        boolean mandanteEraMandanteUltimo = ultimo.mandanteJogadorId().equals(mandanteId);
        int golsMandanteUltimo = mandanteEraMandanteUltimo ? nz(ultimo.golsMandante()) : nz(ultimo.golsVisitante());
        int golsVisitanteUltimo = mandanteEraMandanteUltimo ? nz(ultimo.golsVisitante()) : nz(ultimo.golsMandante());
        int saldoUltimo = golsMandanteUltimo - golsVisitanteUltimo;

        if (Math.abs(saldoUltimo) >= 3) {
            String vencedorUltimo = saldoUltimo > 0 ? nomeM : nomeV;
            ctx.mensagens.add(vencedorUltimo + " aplicou uma goleada no último confronto direto ("
                    + golsMandanteUltimo + "x" + golsVisitanteUltimo + "), histórico recente pesa a favor.");
        } else if (h2h != null && h2h.getTotalJogos() >= 2) {
            int diff = h2h.getVitoriasMandanteAtual() - h2h.getVitoriasVisitanteAtual();
            if (diff >= 3) {
                ctx.mensagens.add(nomeM + " tem ampla paternidade histórica sobre " + nomeV + ".");
            } else if (diff <= -3) {
                ctx.mensagens.add(nomeV + " costuma levar a melhor nos confrontos diretos.");
            } else if (h2h.getEmpates() > (h2h.getTotalJogos() * 0.6)) {
                ctx.mensagens.add("Histórico de muitos empates entre os dois.");
                ctx.reforcoEmpate += 0.15;
            } else {
                ctx.mensagens.add("Histórico direto equilibrado entre os dois.");
            }
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
            ctx.skewGols -= 0.14;
            ctx.reforcoEmpate -= 0.10;
        } else if (saldo == -3) {
            ctx.mensagens.add("Ida foi uma goleada (" + placarIda + "). Virada é improvável, mas não impossível.");
            ctx.skewGols -= 0.11;
        } else if (saldo == -2) {
            ctx.mensagens.add("Mandante perdeu por dois gols de diferença na ida (" + placarIda + "). Precisa de uma boa atuação para reverter.");
            ctx.skewGols -= 0.08;
        } else if (saldo == -1) {
            if (totalGolsIda >= 5) {
                ctx.mensagens.add("Ida movimentada e apertada (" + placarIda + "). Mandante saiu atrás por pouco, mas o confronto foi aberto.");
            } else {
                ctx.mensagens.add("Mandante pressionado pela vitória após derrota mínima na ida (" + placarIda + ").");
            }
            ctx.skewGols -= 0.04;
        } else if (saldo == 0) {
            if (totalGolsIda == 0) {
                ctx.mensagens.add("Ida travada e sem gols (0x0). Confronto decidido praticamente do zero na volta.");
            } else {
                ctx.mensagens.add("Ida terminou empatada (" + placarIda + "). Tudo em aberto para a decisão.");
            }
            ctx.mensagens.add("Confronto totalmente aberto.");
        } else if (saldo == 1) {
            ctx.mensagens.add("Mandante vem de vitória apertada na ida (" + placarIda + "). Vantagem mínima, nada garantido ainda.");
            ctx.skewGols += 0.03;
        } else if (saldo == 2) {
            ctx.mensagens.add("Mandante confortável com a vantagem construída na ida (" + placarIda + ").");
            ctx.skewGols += 0.08;
            ctx.reforcoEmpate += 0.10;
        } else if (saldo == 3) {
            ctx.mensagens.add("Ida foi uma goleada a favor do mandante (" + placarIda + "). Vantagem muito confortável.");
            ctx.skewGols += 0.11;
        } else { // saldo >= 4
            ctx.mensagens.add("Goleada aplicada na ida (" + placarIda + "). Classificação praticamente encaminhada.");
            ctx.skewGols += 0.14;
            ctx.reforcoEmpate += 0.10;
        }
    }

    private ResultadoIda analisarJogoIda(String partidaVoltaId, String mandanteVoltaId, String visitanteVoltaId, FaseMataMata etapaMataMata) {
        Optional<PartidaIdaResultadoDTO> idaOpt = partidaRepository
                .buscarResultadoPartidaIdaPorProximaPartida(partidaVoltaId, mandanteVoltaId, visitanteVoltaId, etapaMataMata);

        if (idaOpt.isEmpty()) {
            return new ResultadoIda(false, 0, 0, 0);
        }

        PartidaIdaResultadoDTO ida = idaOpt.get();
        int golsMandanteAtualNaIda = nz(ida.golsVisitante());
        int golsVisitanteAtualNaIda = nz(ida.golsMandante());
        int saldo = golsMandanteAtualNaIda - golsVisitanteAtualNaIda;

        return new ResultadoIda(true, saldo, golsMandanteAtualNaIda, golsVisitanteAtualNaIda);
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

    private double mediaGolsPorJogo(Integer golsTemporada, Integer jogosTemporada, Integer golsCarreira, Integer jogosCarreira) {
        if (jogosTemporada != null && jogosTemporada >= 3 && golsTemporada != null) {
            return golsTemporada / (double) jogosTemporada;
        }
        if (jogosCarreira != null && jogosCarreira > 0 && golsCarreira != null) {
            return golsCarreira / (double) jogosCarreira;
        }
        return MEDIA_GOLS_LIGA / 2.0;
    }

    // ---- Núcleo unificado: uma única matriz de Poisson alimenta tanto o V/E/D quanto o placar cotado ----

    private record MatrizPoisson(double[][] probabilidades, double somaTotal) {
        double probVitoriaMandante() {
            double soma = 0;
            for (int m = 0; m <= MAX_GOLS_MATRIZ; m++)
                for (int v = 0; v <= MAX_GOLS_MATRIZ; v++)
                    if (m > v) soma += probabilidades[m][v];
            return soma / somaTotal;
        }
        double probEmpate() {
            double soma = 0;
            for (int i = 0; i <= MAX_GOLS_MATRIZ; i++) soma += probabilidades[i][i];
            return soma / somaTotal;
        }
        double probVitoriaVisitante() {
            double soma = 0;
            for (int m = 0; m <= MAX_GOLS_MATRIZ; m++)
                for (int v = 0; v <= MAX_GOLS_MATRIZ; v++)
                    if (m < v) soma += probabilidades[m][v];
            return soma / somaTotal;
        }
    }

    private MatrizPoisson montarMatrizPoisson(double expectativaMandante, double expectativaVisitante) {
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
        return new MatrizPoisson(matriz, somaTotal);
    }

    /**
     * Reforça (ou reduz) a probabilidade de empate multiplicando a diagonal da matriz,
     * depois renormaliza tudo para continuar somando 100%. Substitui a antiga fórmula
     * gaussiana desacoplada — agora o ajuste de contexto atua sobre a mesma matriz que
     * gera o placar, então V/E/D e placar cotado nunca mais podem se contradizer.
     */
    private MatrizPoisson aplicarReforcoEmpate(MatrizPoisson original, double reforco) {
        double fator = 1.0 + reforco; // reforco positivo aumenta empate, negativo reduz
        double[][] nova = new double[MAX_GOLS_MATRIZ + 1][MAX_GOLS_MATRIZ + 1];
        double somaTotal = 0.0;

        for (int m = 0; m <= MAX_GOLS_MATRIZ; m++) {
            for (int v = 0; v <= MAX_GOLS_MATRIZ; v++) {
                double valor = original.probabilidades()[m][v];
                if (m == v) valor *= Math.max(0.3, fator);
                nova[m][v] = valor;
                somaTotal += valor;
            }
        }
        return new MatrizPoisson(nova, somaTotal);
    }

    private ProbabilidadePartidaDTO.PlacarCotadoDTO extrairPlacarCotado(
            MatrizPoisson matriz, double expectativaMandante, double expectativaVisitante) {

        int melhorGolsM = 0, melhorGolsV = 0;
        double melhorProb = -1;
        double probAmbosMarcam = 0.0;
        double probMaisDe2Meio = 0.0;

        List<ProbabilidadePartidaDTO.PlacarProvavelDTO> candidatos = new ArrayList<>();

        for (int golsM = 0; golsM <= MAX_GOLS_MATRIZ; golsM++) {
            for (int golsV = 0; golsV <= MAX_GOLS_MATRIZ; golsV++) {
                double probNormalizada = matriz.probabilidades()[golsM][golsV] / matriz.somaTotal();

                if (probNormalizada > melhorProb) {
                    melhorProb = probNormalizada;
                    melhorGolsM = golsM;
                    melhorGolsV = golsV;
                }
                if (golsM >= 1 && golsV >= 1) probAmbosMarcam += probNormalizada;
                if (golsM + golsV >= 3) probMaisDe2Meio += probNormalizada;

                candidatos.add(new ProbabilidadePartidaDTO.PlacarProvavelDTO(golsM, golsV, round2(probNormalizada * 100)));
            }
        }

        List<ProbabilidadePartidaDTO.PlacarProvavelDTO> top3 = candidatos.stream()
                .sorted((a, b) -> Double.compare(b.probabilidade(), a.probabilidade()))
                .limit(3)
                .toList();

        String observacao = String.format(
                "Placar cotado via distribuição de Poisson, já ajustada por forma recente, força dos elencos e histórico direto. "
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

    private double poissonPmf(double lambda, int k) {
        return Math.pow(lambda, k) * Math.exp(-lambda) / fatorial(k);
    }

    private double fatorial(int n) {
        double resultado = 1.0;
        for (int i = 2; i <= n; i++) resultado *= i;
        return resultado;
    }

    private double clamp(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static class ContextoAnalise {
        List<String> mensagens = new ArrayList<>();
        double skewGols = 0.0;      // inclinação adicional de gols a favor do mandante (+) ou visitante (-)
        double reforcoEmpate = 0.0; // multiplicador extra sobre a diagonal de empate da matriz
    }

    private record ResultadoIda(boolean temJogoAnterior, int saldoMandanteAtual, int golsIdaMandanteAtual, int golsIdaVisitanteAtual) {}
}