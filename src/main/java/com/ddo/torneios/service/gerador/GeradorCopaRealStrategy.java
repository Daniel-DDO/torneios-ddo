package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class GeradorCopaRealStrategy extends GeradorMataMataBase implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        throw new UnsupportedOperationException("Para Copa Real, use o método especializado recebendo 3 listas: Elite, Intermediários e Resto.");
    }

    public List<Partida> gerar(FaseTorneio fase,
                               List<ParticipacaoFase> elite,
                               List<ParticipacaoFase> intermediarios,
                               List<ParticipacaoFase> resto) {

        if (elite.size() != 8) throw new IllegalArgumentException("Elite deve ter 8 jogadores.");
        if (intermediarios.size() != 8) throw new IllegalArgumentException("Intermediários deve ter 8 jogadores.");

        List<ParticipacaoFase> eliteShuffle = new ArrayList<>(elite);
        Collections.shuffle(eliteShuffle);

        List<ParticipacaoFase> intermedShuffle = new ArrayList<>(intermediarios);
        Collections.shuffle(intermedShuffle);

        List<ParticipacaoFase> restoShuffle = new ArrayList<>(resto);
        Collections.shuffle(restoShuffle);

        List<Partida> todasPartidas = new ArrayList<>();

        ResultadoProcessamento resultadoResto = reduzirRestoParaOito(fase, restoShuffle);
        todasPartidas.addAll(resultadoResto.partidasCriadas);
        List<SlotCompetidor> sobreviventesResto = resultadoResto.slotsSobreviventes;

        List<Partida> oitavas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ParticipacaoFase rei = eliteShuffle.get(i);
            List<Partida> confronto = criarConfronto(fase, i + 1, rei, null, "Oitavas de Final");
            confronto.forEach(p -> p.setEtapaMataMata(FaseMataMata.OITAVAS));
            oitavas.addAll(confronto);
        }

        for (int i = 0; i < 8; i++) {
            ParticipacaoFase chefeIntermediario = intermedShuffle.get(i);

            SlotCompetidor desafiante = (i < sobreviventesResto.size()) ? sobreviventesResto.get(i) : null;

            if (desafiante != null) {
                ParticipacaoFase p1 = chefeIntermediario;
                ParticipacaoFase p2 = desafiante.jogador;

                List<Partida> confronto16avos = criarConfronto(fase, i + 1, p1, p2, "16-avos de Final");
                confronto16avos.forEach(p -> p.setEtapaMataMata(FaseMataMata.DEZESSEIS_AVOS));
                todasPartidas.addAll(confronto16avos);

                if (desafiante.partidaAnterior != null) {
                    conectarVencedor(desafiante.partidaAnterior, buscarJogoEntrada(confronto16avos), 1);
                }

                Partida mestre16avos = buscarJogoMestre(confronto16avos);
                Partida entradaOitava = buscarJogoEntrada(oitavas, i + 1);
                conectarVencedor(mestre16avos, entradaOitava, 1);

            } else {
                atualizarConfrontoOitavas(oitavas, i + 1, chefeIntermediario);
            }
        }

        vincularProximasFases(oitavas, fase);

        todasPartidas.addAll(oitavas);

        return todasPartidas;
    }

    private ResultadoProcessamento reduzirRestoParaOito(FaseTorneio fase, List<ParticipacaoFase> resto) {
        List<Partida> partidasGeradas = new ArrayList<>();
        List<SlotCompetidor> slotsAtuais = resto.stream().map(SlotCompetidor::new).collect(Collectors.toList());

        while (slotsAtuais.size() > 8) {
            int n = slotsAtuais.size();
            FaseMataMata etapa = calcularEtapa(n);
            int target = calcularProximoTarget(n);

            if (target < 8) target = 8;

            int jogosNecessarios = n - target;
            int numByes = target - jogosNecessarios;

            List<SlotCompetidor> proximaFaseSlots = new ArrayList<>();

            for (int i = 0; i < numByes; i++) {
                proximaFaseSlots.add(slotsAtuais.remove(0));
            }

            for (int i = 0; i < jogosNecessarios; i++) {
                SlotCompetidor s1 = slotsAtuais.remove(0);
                SlotCompetidor s2 = slotsAtuais.remove(0);

                int chaveIndex = numByes + i + 1;

                List<Partida> confronto = criarConfronto(fase, chaveIndex, s1.jogador, s2.jogador, "Playoff " + etapa.name());
                confronto.forEach(p -> p.setEtapaMataMata(etapa));
                partidasGeradas.addAll(confronto);

                if (s1.partidaAnterior != null) conectarVencedor(s1.partidaAnterior, buscarJogoEntrada(confronto), 2); // p1 é visitante na ida
                if (s2.partidaAnterior != null) conectarVencedor(s2.partidaAnterior, buscarJogoEntrada(confronto), 1); // p2 é mandante na ida

                proximaFaseSlots.add(new SlotCompetidor(buscarJogoMestre(confronto)));
            }
            slotsAtuais = proximaFaseSlots;
        }

        return new ResultadoProcessamento(partidasGeradas, slotsAtuais);
    }

    private FaseMataMata calcularEtapa(int n) {
        if (n <= 16) return FaseMataMata.TRINTA_E_DOIS_AVOS;
        if (n <= 32) return FaseMataMata.SESSENTA_E_QUATRO_AVOS;
        return FaseMataMata.SESSENTA_E_QUATRO_AVOS;
    }

    private int calcularProximoTarget(int n) {
        if (n > 32) return 32;
        if (n > 16) return 16;
        return 8;
    }

    private void atualizarConfrontoOitavas(List<Partida> oitavas, int chaveIndex, ParticipacaoFase oponenteDaElite) {
        oitavas.stream()
                .filter(p -> p.getChaveIndex() == chaveIndex)
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_IDA || p.getTipoPartida() == TipoPartida.MATA_MATA_UNICO)
                .forEach(p -> p.setMandante(oponenteDaElite.getJogadorClube()));

        oitavas.stream()
                .filter(p -> p.getChaveIndex() == chaveIndex)
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_VOLTA)
                .forEach(p -> p.setVisitante(oponenteDaElite.getJogadorClube()));
    }

    private void conectarVencedor(Partida anteriorMestre, Partida proximaEntrada, int slot) {
        anteriorMestre.setProximaPartida(proximaEntrada);
        anteriorMestre.setSlotNaProxima(slot);
    }

    private Partida buscarJogoMestre(List<Partida> lista) {
        return lista.stream()
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_VOLTA || p.getTipoPartida() == TipoPartida.MATA_MATA_UNICO)
                .findFirst().orElse(lista.get(0));
    }

    private Partida buscarJogoEntrada(List<Partida> lista) {
        return lista.stream()
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_IDA || p.getTipoPartida() == TipoPartida.MATA_MATA_UNICO)
                .findFirst().orElse(lista.get(0));
    }

    private Partida buscarJogoEntrada(List<Partida> lista, int chave) {
        return lista.stream()
                .filter(p -> p.getChaveIndex() == chave)
                .filter(p -> p.getTipoPartida() == TipoPartida.MATA_MATA_IDA || p.getTipoPartida() == TipoPartida.MATA_MATA_UNICO)
                .findFirst().orElseThrow();
    }

    private class ResultadoProcessamento {
        List<Partida> partidasCriadas;
        List<SlotCompetidor> slotsSobreviventes;

        public ResultadoProcessamento(List<Partida> p, List<SlotCompetidor> s) {
            this.partidasCriadas = p;
            this.slotsSobreviventes = s;
        }
    }

    private class SlotCompetidor {
        ParticipacaoFase jogador;
        Partida partidaAnterior;
        public SlotCompetidor(ParticipacaoFase p) { this.jogador = p; }
        public SlotCompetidor(Partida p) { this.partidaAnterior = p; }
    }
}