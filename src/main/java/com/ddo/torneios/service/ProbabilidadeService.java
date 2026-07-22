package com.ddo.torneios.service;

import com.ddo.torneios.dto.HistoricoConfrontoProjection;
import com.ddo.torneios.dto.ProbabilidadePartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public ProbabilidadePartidaDTO calcularProbabilidade(Partida partida) {
        if (partida.isRealizada()) return new ProbabilidadePartidaDTO(0, 0, 0, "Partida finalizada.");
        if (partida.getMandante() == null || partida.getVisitante() == null) {
            return new ProbabilidadePartidaDTO(50, 0, 50, "Aguardando oponentes.");
        }

        JogadorClube mandante = partida.getMandante();
        JogadorClube visitante = partida.getVisitante();

        HistoricoConfrontoProjection h2h = partidaRepository.findResumoConfrontoDireto(
                mandante.getJogador().getId(),
                visitante.getJogador().getId()
        );

        double scoreM = calcularScoreTotal(mandante, h2h, true);
        double scoreV = calcularScoreTotal(visitante, h2h, false);

        scoreM *= FATOR_CASA;

        ContextoAnalise contexto = new ContextoAnalise();
        analisarTabu(h2h, mandante.getJogador().getNome(), visitante.getJogador().getNome(), contexto);

        TipoPartida tipo = partida.getTipoPartida();

        if (tipo == TipoPartida.MATA_MATA_VOLTA || tipo == TipoPartida.FINAL_VOLTA) {
            ResultadoIda resultadoIda = analisarJogoIda(partida);
            if (resultadoIda.temJogoAnterior) {
                aplicarLogicaJogoVolta(resultadoIda.saldoMandanteAtual, contexto);
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

        return new ProbabilidadePartidaDTO(
                (int) Math.round(probMandante),
                (int) Math.round(probEmpate),
                (int) Math.round(probVisitante),
                analiseFinal.trim()
        );
    }

    private double calcularScoreTotal(JogadorClube jc, HistoricoConfrontoProjection h2h, boolean isMandanteNoH2H) {
        double estrelas = (jc.getClube().getEstrelas() != null) ? jc.getClube().getEstrelas().doubleValue() : 3.0;
        double sClube = estrelas * 20.0;

        Jogador j = jc.getJogador();
        double winRate = (j.getPartidasJogadas() > 0) ? (double) j.getVitorias() / j.getPartidasJogadas() : 0.5;
        double sHistorico = winRate * 100.0;

        double sMomento = sHistorico;
        if (jc.getPartidasJogadas() != null && jc.getPartidasJogadas() >= 3) {
            sMomento = (jc.getAproveitamento() != null) ? jc.getAproveitamento() : 50.0;
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

    private void aplicarLogicaJogoVolta(int saldo, ContextoAnalise ctx) {
        if (saldo <= -3) {
            ctx.mensagens.add("Mandante precisa de um milagre.");
            ctx.ajusteEmpate -= 15.0;
        } else if (saldo <= -1) {
            ctx.mensagens.add("Mandante pressionado pela vitória.");
            ctx.ajusteEmpate -= 5.0;
            ctx.ajusteMandante += 5.0;
        } else if (saldo >= 2) {
            ctx.mensagens.add("Mandante confortável com a vantagem.");
            ctx.ajusteEmpate += 15.0;
            ctx.ajusteMandante -= 5.0;
        } else {
            ctx.mensagens.add("Confronto totalmente aberto.");
        }
    }

    private ResultadoIda analisarJogoIda(Partida partida) {
        TipoPartida tipoIda = (partida.getTipoPartida() == TipoPartida.FINAL_VOLTA)
                ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA;

        Optional<Partida> partidaIdaOpt = partidaRepository.findPartidaIda(
                partida.getFase().getId(),
                partida.getChaveIndex(),
                tipoIda
        );

        if (partidaIdaOpt.isPresent() && partidaIdaOpt.get().isRealizada()) {
            Partida ida = partidaIdaOpt.get();
            int gm = ida.getGolsMandante() != null ? ida.getGolsMandante() : 0;
            int gv = ida.getGolsVisitante() != null ? ida.getGolsVisitante() : 0;
            return new ResultadoIda(true, gv - gm);
        }
        return new ResultadoIda(false, 0);
    }

    private static class ContextoAnalise {
        List<String> mensagens = new ArrayList<>();
        double ajusteEmpate = 0.0;
        double ajusteMandante = 0.0;
    }

    private record ResultadoIda(boolean temJogoAnterior, int saldoMandanteAtual) {}
}