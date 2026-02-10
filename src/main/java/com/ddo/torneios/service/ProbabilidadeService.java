package com.ddo.torneios.service;

import com.ddo.torneios.dto.ProbabilidadePartidaDTO;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ProbabilidadeService {

    @Autowired
    private PartidaRepository partidaRepository;

    public ProbabilidadePartidaDTO calcularProbabilidade(Partida partida) {
        if (partida.isRealizada()) return new ProbabilidadePartidaDTO(0, 0, 0, "Partida finalizada.");
        if (partida.getMandante() == null || partida.getVisitante() == null) {
            return new ProbabilidadePartidaDTO(50, 0, 50, "Aguardando oponentes.");
        }

        double scoreM = calcularForcaBase(partida.getMandante()) + 10.0;
        double scoreV = calcularForcaBase(partida.getVisitante());

        double chanceEmpate = 25.0;
        String analise = "Confronto em aberto.";

        TipoPartida tipo = partida.getTipoPartida();

        if (tipo == TipoPartida.MATA_MATA_VOLTA || tipo == TipoPartida.FINAL_VOLTA) {

            TipoPartida tipoIdaCorrespondente = (tipo == TipoPartida.FINAL_VOLTA)
                    ? TipoPartida.FINAL_IDA
                    : TipoPartida.MATA_MATA_IDA;

            Optional<Partida> partidaIdaOpt = partidaRepository.findPartidaIda(
                    partida.getFase().getId(),
                    partida.getChaveIndex(),
                    tipoIdaCorrespondente
            );

            if (partidaIdaOpt.isPresent()) {
                Partida ida = partidaIdaOpt.get();

                if (ida.isRealizada()) {
                    int golsMandanteIda = ida.getGolsMandante() != null ? ida.getGolsMandante() : 0;
                    int golsVisitanteIda = ida.getGolsVisitante() != null ? ida.getGolsVisitante() : 0;

                    int saldoAtualMandante = golsVisitanteIda - golsMandanteIda;

                    if (saldoAtualMandante <= -2) {
                        analise = "Mandante pressionado: Precisa reverter o placar.";
                        scoreM += 20.0;
                        chanceEmpate -= 15.0;

                    } else if (saldoAtualMandante >= 2) {
                        analise = "Mandante confortável: Pode jogar pelo empate.";
                        scoreM -= 5.0;
                        chanceEmpate += 15.0;
                    } else {
                        analise = "Tudo indefinido após o jogo de ida.";
                    }
                }
            }
        }
        else if (tipo == TipoPartida.MATA_MATA_UNICO || tipo == TipoPartida.FINAL_UNICA) {
            analise = "Decisão em jogo único. Probabilidade de pênaltis considerada.";
            chanceEmpate += 5.0;
        }

        double totalForca = scoreM + scoreV;

        if (totalForca <= 0) totalForca = 1;

        double pctMandante = (scoreM / totalForca) * (100 - chanceEmpate);
        double pctVisitante = (scoreV / totalForca) * (100 - chanceEmpate);

        return new ProbabilidadePartidaDTO(
                (int) Math.round(pctMandante),
                (int) Math.round(chanceEmpate),
                (int) Math.round(pctVisitante),
                analise
        );
    }

    private double calcularForcaBase(JogadorClube jc) {
        double forcaClube = 50.0;
        if (jc.getClube().getEstrelas() != null) {
            forcaClube = jc.getClube().getEstrelas().doubleValue() * 20.0;
        }

        Jogador j = jc.getJogador();
        double winRate = 0.5;
        if (j.getPartidasJogadas() != null && j.getPartidasJogadas() > 0) {
            winRate = (double) j.getVitorias() / j.getPartidasJogadas();
        }

        double momento = 0.5;
        if (jc.getAproveitamento() != null) {
            momento = jc.getAproveitamento() / 100.0;
        }

        return (forcaClube * 0.4) + ((winRate * 100) * 0.4) + ((momento * 100) * 0.2);
    }
}