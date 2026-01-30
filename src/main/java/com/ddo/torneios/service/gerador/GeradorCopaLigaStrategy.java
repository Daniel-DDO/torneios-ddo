package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeradorCopaLigaStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int n = participantes.size();

        if (n != 8) {
            throw new IllegalArgumentException("Para a Copa Liga, é necessário exatamente 8 participantes nos Playoffs iniciais (Oitavas).");
        }

        List<ParticipacaoFase> sorteio = new ArrayList<>(participantes);
        Collections.shuffle(sorteio);

        List<Partida> todasPartidas = new ArrayList<>();

        FaseMataMata etapaPlayoff = FaseMataMata.OITAVAS;

        List<Partida> partidasPlayoffs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            // Pega par de jogadores
            ParticipacaoFase p1 = sorteio.get(i * 2);
            ParticipacaoFase p2 = sorteio.get(i * 2 + 1);

            List<Partida> confronto = criarConfronto(fase, i + 1, p1, p2, "Playoff");

            confronto.forEach(p -> p.setEtapaMataMata(etapaPlayoff));

            partidasPlayoffs.addAll(confronto);
            todasPartidas.addAll(confronto);
        }

        List<Partida> partidasQuartas = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            int chaveIndex = i + 1;

            List<Partida> confrontoQuartas = criarConfrontoVazio(fase, FaseMataMata.QUARTAS, chaveIndex, true);
            partidasQuartas.addAll(confrontoQuartas);
            todasPartidas.addAll(confrontoQuartas);

            Partida decisivaPlayoff = buscarJogoDecisivo(partidasPlayoffs, chaveIndex);
            Partida decisivaQuartas = buscarJogoDecisivo(confrontoQuartas, chaveIndex);

            decisivaPlayoff.setProximaPartida(decisivaQuartas);
            decisivaPlayoff.setSlotNaProxima(2);
        }

        List<Partida> partidasSemis = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int chaveIndex = i + 1;

            List<Partida> confrontoSemi = criarConfrontoVazio(fase, FaseMataMata.SEMIFINAL, chaveIndex, true);
            partidasSemis.addAll(confrontoSemi);
            todasPartidas.addAll(confrontoSemi);

            Partida decisivaSemi = buscarJogoDecisivo(confrontoSemi, chaveIndex);

            Partida qfA = buscarJogoDecisivo(partidasQuartas, (i * 2) + 1);
            qfA.setProximaPartida(decisivaSemi);
            qfA.setSlotNaProxima(1);

            Partida qfB = buscarJogoDecisivo(partidasQuartas, (i * 2) + 2);
            qfB.setProximaPartida(decisivaSemi);
            qfB.setSlotNaProxima(2);
        }

        boolean finalUnica = Boolean.TRUE.equals(fase.getFinalJogoUnico());
        List<Partida> confrontoFinal = criarConfrontoVazio(fase, FaseMataMata.FINAL, 1, !finalUnica);
        todasPartidas.addAll(confrontoFinal);

        Partida jogoFinal = buscarJogoDecisivo(confrontoFinal, 1);

        Partida semi1 = buscarJogoDecisivo(partidasSemis, 1);
        semi1.setProximaPartida(jogoFinal);
        semi1.setSlotNaProxima(1);

        Partida semi2 = buscarJogoDecisivo(partidasSemis, 2);
        semi2.setProximaPartida(jogoFinal);
        semi2.setSlotNaProxima(2);

        return todasPartidas;
    }

    private List<Partida> criarConfrontoVazio(FaseTorneio fase, FaseMataMata etapa, int chaveIndex, boolean temVolta) {
        List<Partida> lista = new ArrayList<>();
        boolean ehFinal = (etapa == FaseMataMata.FINAL);

        if (temVolta) {
            Partida ida = new Partida();
            ida.setFase(fase);
            ida.setEtapaMataMata(etapa);
            ida.setChaveIndex(chaveIndex);
            ida.setTipoPartida(ehFinal ? TipoPartida.FINAL_IDA : TipoPartida.MATA_MATA_IDA);
            ida.setLogEventos("Aguardando definição dos participantes...");
            lista.add(ida);

            Partida volta = new Partida();
            volta.setFase(fase);
            volta.setEtapaMataMata(etapa);
            volta.setChaveIndex(chaveIndex);
            volta.setTipoPartida(ehFinal ? TipoPartida.FINAL_VOLTA : TipoPartida.MATA_MATA_VOLTA);
            volta.setLogEventos("Aguardando definição dos participantes...");

            if (ehFinal && fase.getEstadioFinal() != null) {
                volta.setEstadio(fase.getEstadioFinal());
            }
            lista.add(volta);
        } else {
            Partida unico = new Partida();
            unico.setFase(fase);
            unico.setEtapaMataMata(etapa);
            unico.setChaveIndex(chaveIndex);
            unico.setTipoPartida(ehFinal ? TipoPartida.FINAL_UNICA : TipoPartida.MATA_MATA_UNICO);
            unico.setLogEventos("Aguardando definição dos participantes...");

            if (ehFinal && fase.getEstadioFinal() != null) {
                unico.setEstadio(fase.getEstadioFinal());
            }
            lista.add(unico);
        }
        return lista;
    }

    private Partida buscarJogoDecisivo(List<Partida> partidas, int chaveIndex) {
        return partidas.stream()
                .filter(p -> p.getChaveIndex() == chaveIndex)
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_VOLTA
                        || p.getTipoPartida() == TipoPartida.MATA_MATA_UNICO
                        || p.getTipoPartida() == TipoPartida.FINAL_VOLTA
                        || p.getTipoPartida() == TipoPartida.FINAL_UNICA)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Não encontrei jogo decisivo para chave " + chaveIndex));
    }

}