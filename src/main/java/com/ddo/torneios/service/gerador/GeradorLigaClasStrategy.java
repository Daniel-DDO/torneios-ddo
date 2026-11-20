package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorLigaClasStrategy implements GeradorPartidasStrategy<Rodada> {

    private static final int MAX_TENTATIVAS = 300;
    private static final int MAX_NOS_POR_TENTATIVA = 20_000;

    private final Random random = new Random();

    @Override
    public List<Rodada> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        validar(fase, participantes);
        int n = participantes.size();
        int r = fase.getNumeroRodadas();

        List<ParticipacaoFase> lista = new ArrayList<>(participantes);
        Collections.shuffle(lista, random);

        Map<String, Integer> indiceEquipe = new HashMap<>();
        int[] equipe = new int[n];
        for (int i = 0; i < n; i++) {
            String id = lista.get(i).getEquipe().getId(); // getId() não inicializa o proxy LAZY
            Integer idx = indiceEquipe.get(id);
            if (idx == null) {
                idx = indiceEquipe.size();
                indiceEquipe.put(id, idx);
            }
            equipe[i] = idx;
        }

        List<List<int[]>> confrontos = null;
        if (indiceEquipe.size() == 2) {
            confrontos = quadradoLatino(equipe, r);
        } else {
            for (int t = 0; t < MAX_TENTATIVAS && confrontos == null; t++) {
                confrontos = sortearRodadas(equipe, r);
            }
        }
        if (confrontos == null) {
            throw new IllegalStateException(
                    "Não foi possível gerar a tabela com esses parâmetros. Tente reduzir o número de rodadas.");
        }

        return montarRodadas(fase, lista, confrontos, orientarMando(n, confrontos));
    }

    // 2 equipes: rodada com deslocamento s => A[i] x B[(i+s) % N]. Deslocamentos distintos nunca repetem adversário.
    private List<List<int[]>> quadradoLatino(int[] equipe, int r) {
        List<Integer> a = new ArrayList<>(), b = new ArrayList<>();
        for (int i = 0; i < equipe.length; i++) (equipe[i] == 0 ? a : b).add(i);
        Collections.shuffle(a, random);
        Collections.shuffle(b, random);
        int tam = a.size();

        List<Integer> deslocamentos = new ArrayList<>();
        for (int s = 0; s < tam; s++) deslocamentos.add(s);
        Collections.shuffle(deslocamentos, random);

        List<List<int[]>> rodadas = new ArrayList<>();
        for (int k = 0; k < r; k++) {
            int s = deslocamentos.get(k);
            List<int[]> jogos = new ArrayList<>();
            for (int i = 0; i < tam; i++) jogos.add(new int[]{a.get(i), b.get((i + s) % tam)});
            rodadas.add(jogos);
        }
        return rodadas;
    }

    // 3+ equipes: busca aleatória com orçamento de nós; quem chama retenta.
    private List<List<int[]>> sortearRodadas(int[] equipe, int r) {
        int n = equipe.length;
        boolean[][] jogou = new boolean[n][n];
        int[] orcamento = {MAX_NOS_POR_TENTATIVA};
        List<List<int[]>> rodadas = new ArrayList<>();
        for (int k = 0; k < r; k++) {
            boolean[] livre = new boolean[n];
            Arrays.fill(livre, true);
            List<int[]> jogos = new ArrayList<>();
            if (!emparelhar(livre, equipe, jogou, jogos, orcamento)) return null;
            rodadas.add(jogos);
        }
        return rodadas;
    }

    private boolean emparelhar(boolean[] livre, int[] equipe, boolean[][] jogou,
                               List<int[]> jogos, int[] orcamento) {
        if (--orcamento[0] < 0) return false;

        int a = -1;
        for (int i = 0; i < livre.length; i++) if (livre[i]) { a = i; break; }
        if (a == -1) return true;

        livre[a] = false;
        List<Integer> candidatos = new ArrayList<>();
        for (int b = 0; b < livre.length; b++) {
            if (livre[b] && equipe[b] != equipe[a] && !jogou[a][b]) candidatos.add(b);
        }
        Collections.shuffle(candidatos, random);

        for (int b : candidatos) {
            livre[b] = false;
            jogou[a][b] = jogou[b][a] = true;
            jogos.add(new int[]{a, b});
            if (emparelhar(livre, equipe, jogou, jogos, orcamento)) return true;
            jogos.remove(jogos.size() - 1);
            jogou[a][b] = jogou[b][a] = false;
            livre[b] = true;
        }
        livre[a] = true;
        return false;
    }

    /** Para cada confronto, true se o 1º índice é o mandante. Todo vértice termina com R/2 em casa. */
    private boolean[][] orientarMando(int n, List<List<int[]>> confrontos) {
        List<int[]> arestas = new ArrayList<>(); // {a, b, rodada, jogo}
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());

        boolean[][] primeiroManda = new boolean[confrontos.size()][];
        for (int r = 0; r < confrontos.size(); r++) {
            primeiroManda[r] = new boolean[confrontos.get(r).size()];
            for (int j = 0; j < confrontos.get(r).size(); j++) {
                int[] c = confrontos.get(r).get(j);
                int id = arestas.size();
                arestas.add(new int[]{c[0], c[1], r, j});
                adj.get(c[0]).add(id);
                adj.get(c[1]).add(id);
            }
        }

        boolean[] usada = new boolean[arestas.size()];
        for (int v = 0; v < n; v++) {
            boolean andou = true;
            while (andou) {
                andou = false;
                int atual = v;
                while (true) {
                    int e = -1;
                    for (int cand : adj.get(atual)) if (!usada[cand]) { e = cand; break; }
                    if (e == -1) break;
                    usada[e] = true;
                    andou = true;
                    int[] ar = arestas.get(e);
                    primeiroManda[ar[2]][ar[3]] = (ar[0] == atual); // quem "sai" é o mandante
                    atual = (ar[0] == atual) ? ar[1] : ar[0];
                }
            }
        }
        return primeiroManda;
    }

    private List<Rodada> montarRodadas(FaseTorneio fase, List<ParticipacaoFase> lista,
                                       List<List<int[]>> confrontos, boolean[][] primeiroManda) {
        List<Rodada> rodadas = new ArrayList<>();
        for (int r = 0; r < confrontos.size(); r++) {
            Rodada rodada = new Rodada();
            rodada.setFase(fase);
            rodada.setNumero(r + 1);
            rodada.setStatus(StatusRodada.ABERTA);

            List<Partida> partidas = new ArrayList<>();
            for (int j = 0; j < confrontos.get(r).size(); j++) {
                int[] c = confrontos.get(r).get(j);
                ParticipacaoFase mandante = lista.get(primeiroManda[r][j] ? c[0] : c[1]);
                ParticipacaoFase visitante = lista.get(primeiroManda[r][j] ? c[1] : c[0]);

                Partida p = new Partida();
                p.setFase(fase);
                p.setRodada(rodada);
                p.setMandante(mandante.getJogadorClube());
                p.setVisitante(visitante.getJogadorClube());
                p.setTipoPartida(TipoPartida.PONTOS_CORRIDOS);
                p.setRealizada(false);
                partidas.add(p);
            }
            Collections.shuffle(partidas, random);
            rodada.setPartidas(partidas);
            rodadas.add(rodada);
        }
        return rodadas;
    }

    private void validar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        Integer r = fase.getNumeroRodadas();
        if (r == null) throw new IllegalArgumentException("O número de rodadas não foi definido na Fase.");

        Map<String, Integer> porEquipe = new HashMap<>();
        for (ParticipacaoFase p : participantes) {
            if (p.getId() == null)
                throw new IllegalStateException("Os participantes precisam estar salvos antes de gerar a tabela.");
            if (p.getEquipe() == null)
                throw new IllegalArgumentException("Todo participante precisa pertencer a uma equipe.");
            porEquipe.merge(p.getEquipe().getId(), 1, Integer::sum);
        }

        if (porEquipe.size() < 2)
            throw new IllegalArgumentException("São necessárias pelo menos 2 equipes.");
        if (porEquipe.values().stream().distinct().count() != 1)
            throw new IllegalArgumentException("Todas as equipes devem ter o mesmo número de jogadores.");
        if (porEquipe.values().iterator().next() % 2 != 0)
            throw new IllegalArgumentException("Cada equipe deve ter um número PAR de jogadores.");

        int max = participantes.size() / 2;
        if (r % 2 != 0)
            throw new IllegalArgumentException("O número de rodadas deve ser PAR para o mando ficar balanceado.");
        if (r < 2 || r > max)
            throw new IllegalArgumentException("Rodadas devem estar entre 2 e " + max + ".");
    }
}