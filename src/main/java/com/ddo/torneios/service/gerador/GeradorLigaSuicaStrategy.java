package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorLigaSuicaStrategy implements GeradorPartidasStrategy<Partida> {

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        int nParticipantes = participantes.size();
        int nRodadasTotal = fase.getNumeroRodadas() != null ? fase.getNumeroRodadas() : 0;

        if (nParticipantes % 2 != 0 || nRodadasTotal % 2 != 0) {
            throw new IllegalArgumentException("Número de participantes e rodadas deve ser PAR.");
        }

        int ultimaRodada = obterUltimaRodada(fase);
        int proximaImpar = ultimaRodada + 1;
        int proximaPar = ultimaRodada + 2;

        if (proximaPar > nRodadasTotal) {
            throw new IllegalStateException("Limite de rodadas atingido.");
        }

        Map<String, Set<String>> historico = carregarHistoricoConfrontos(fase);
        Map<String, Integer> mandosAcumulados = carregarContagemMando(fase);
        List<Partida> novasPartidas = new ArrayList<>();

        List<ParticipacaoFase> rankingImpar = (ultimaRodada == 0) ?
                new ArrayList<>(participantes) :
                ordenarPorDesempenho(participantes, fase, ultimaRodada - 1);

        if (ultimaRodada == 0) Collections.shuffle(rankingImpar);

        List<ConfrontoBase> paresImpar = parearSuico(rankingImpar, historico);

        Set<String> quemFoiMandanteNaImpar = new HashSet<>();

        for (ConfrontoBase par : paresImpar) {
            Partida p = criarPartidaComMandoEquilibrado(fase, proximaImpar, par.p1, par.p2, mandosAcumulados);
            novasPartidas.add(p);
            quemFoiMandanteNaImpar.add(p.getMandante().getId());
            atualizarMapasTemporarios(historico, p);
        }

        List<ParticipacaoFase> mandantesAnteriores = new ArrayList<>();
        List<ParticipacaoFase> visitantesAnteriores = new ArrayList<>();

        for (ParticipacaoFase p : participantes) {
            if (quemFoiMandanteNaImpar.contains(p.getJogadorClube().getId())) {
                mandantesAnteriores.add(p);
            } else {
                visitantesAnteriores.add(p);
            }
        }

        Comparator<ParticipacaoFase> comp = (a, b) -> {
            int ptsA = (ultimaRodada == 0) ? 0 : obterPontosNaRodada(a, fase, ultimaRodada);
            int ptsB = (ultimaRodada == 0) ? 0 : obterPontosNaRodada(b, fase, ultimaRodada);
            return ptsB - ptsA;
        };

        mandantesAnteriores.sort(comp);
        visitantesAnteriores.sort(comp);
        if (ultimaRodada == 0) {
            Collections.shuffle(mandantesAnteriores);
            Collections.shuffle(visitantesAnteriores);
        }

        for (int i = 0; i < mandantesAnteriores.size(); i++) {
            ParticipacaoFase pAntigoMandante = mandantesAnteriores.get(i);

            ParticipacaoFase pAntigoVisitante = null;
            for (int j = 0; j < visitantesAnteriores.size(); j++) {
                ParticipacaoFase candidato = visitantesAnteriores.get(j);
                if (!jaSeEnfrentaram(pAntigoMandante, candidato, historico)) {
                    pAntigoVisitante = visitantesAnteriores.remove(j);
                    break;
                }
            }

            if (pAntigoVisitante == null) pAntigoVisitante = visitantesAnteriores.remove(0);

            Partida pPar = new Partida();
            pPar.setFase(fase);
            pPar.setMandante(pAntigoVisitante.getJogadorClube());
            pPar.setVisitante(pAntigoMandante.getJogadorClube());
            pPar.setRealizada(false);
            pPar.setLogEventos("Rodada " + proximaPar);
            novasPartidas.add(pPar);

            atualizarMapasTemporarios(historico, pPar);
        }

        return novasPartidas;
    }

    private List<ConfrontoBase> parearSuico(List<ParticipacaoFase> jogadores, Map<String, Set<String>> hist) {
        List<ConfrontoBase> pares = new ArrayList<>();
        List<ParticipacaoFase> fila = new ArrayList<>(jogadores);

        while (fila.size() >= 2) {
            ParticipacaoFase p1 = fila.remove(0);
            ParticipacaoFase p2 = null;

            for (int i = 0; i < fila.size(); i++) {
                if (!jaSeEnfrentaram(p1, fila.get(i), hist)) {
                    p2 = fila.remove(i);
                    break;
                }
            }
            if (p2 == null) p2 = fila.remove(0);
            pares.add(new ConfrontoBase(p1, p2));
        }
        return pares;
    }

    private List<ParticipacaoFase> ordenarPorDesempenho(List<ParticipacaoFase> p, FaseTorneio f, int numRodada) {
        List<ParticipacaoFase> lista = new ArrayList<>(p);
        lista.sort((a, b) -> obterPontosNaRodada(b, f, numRodada) - obterPontosNaRodada(a, f, numRodada));
        return lista;
    }

    private int obterPontosNaRodada(ParticipacaoFase p, FaseTorneio f, int num) {
        if (f.getRodadas() == null) return 0;
        return f.getRodadas().stream()
                .filter(r -> r.getNumero() == num)
                .flatMap(r -> r.getPartidas().stream())
                .filter(partida -> partida.getMandante().getId().equals(p.getJogadorClube().getId()) ||
                        partida.getVisitante().getId().equals(p.getJogadorClube().getId()))
                .map(partida -> {
                    boolean isM = partida.getMandante().getId().equals(p.getJogadorClube().getId());
                    Integer gM = partida.getGolsMandante() != null ? partida.getGolsMandante() : 0;
                    Integer gV = partida.getGolsVisitante() != null ? partida.getGolsVisitante() : 0;
                    if (isM) return gM > gV ? 3 : (gM.equals(gV) ? 1 : 0);
                    return gV > gM ? 3 : (gV.equals(gM) ? 1 : 0);
                }).findFirst().orElse(0);
    }

    private Partida criarPartidaComMandoEquilibrado(FaseTorneio f, int rd, ParticipacaoFase p1, ParticipacaoFase p2, Map<String, Integer> mandos) {
        int m1 = mandos.getOrDefault(p1.getJogadorClube().getId(), 0);
        int m2 = mandos.getOrDefault(p2.getJogadorClube().getId(), 0);

        Partida p = new Partida();
        p.setFase(f);
        p.setLogEventos("Rodada " + rd);
        p.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);
        p.setRealizada(false);

        if (m1 <= m2) {
            p.setMandante(p1.getJogadorClube());
            p.setVisitante(p2.getJogadorClube());
        } else {
            p.setMandante(p2.getJogadorClube());
            p.setVisitante(p1.getJogadorClube());
        }
        return p;
    }

    private void atualizarMapasTemporarios(Map<String, Set<String>> hist, Partida p) {
        String idM = p.getMandante().getId();
        String idV = p.getVisitante().getId();
        hist.computeIfAbsent(idM, k -> new HashSet<>()).add(idV);
        hist.computeIfAbsent(idV, k -> new HashSet<>()).add(idM);
    }

    private boolean jaSeEnfrentaram(ParticipacaoFase a, ParticipacaoFase b, Map<String, Set<String>> hist) {
        return hist.getOrDefault(a.getJogadorClube().getId(), new HashSet<>()).contains(b.getJogadorClube().getId());
    }

    private Map<String, Set<String>> carregarHistoricoConfrontos(FaseTorneio f) {
        Map<String, Set<String>> hist = new HashMap<>();
        if (f.getRodadas() == null) return hist;
        f.getRodadas().forEach(r -> r.getPartidas().forEach(p -> {
            hist.computeIfAbsent(p.getMandante().getId(), k -> new HashSet<>()).add(p.getVisitante().getId());
            hist.computeIfAbsent(p.getVisitante().getId(), k -> new HashSet<>()).add(p.getMandante().getId());
        }));
        return hist;
    }

    private Map<String, Integer> carregarContagemMando(FaseTorneio f) {
        Map<String, Integer> m = new HashMap<>();
        if (f.getRodadas() == null) return m;
        f.getRodadas().forEach(r -> r.getPartidas().forEach(p ->
                m.put(p.getMandante().getId(), m.getOrDefault(p.getMandante().getId(), 0) + 1)
        ));
        return m;
    }

    private int obterUltimaRodada(FaseTorneio f) {
        if (f.getRodadas() == null || f.getRodadas().isEmpty()) return 0;
        return f.getRodadas().stream().mapToInt(Rodada::getNumero).max().getAsInt();
    }

    private static class ConfrontoBase {
        ParticipacaoFase p1; ParticipacaoFase p2;
        ConfrontoBase(ParticipacaoFase p1, ParticipacaoFase p2) { this.p1 = p1; this.p2 = p2; }
    }
}