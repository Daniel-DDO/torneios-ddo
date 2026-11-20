package com.ddo.torneios.service.gerador;

import com.ddo.torneios.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GeradorMataMataClasStrategy extends GeradorMataMataBase
        implements GeradorPartidasStrategy<Partida> {

    private static final Comparator<ParticipacaoFase> POR_POSICAO =
            Comparator.comparing(ParticipacaoFase::getPosicaoClassificacao,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private final Random random = new Random();

    @Override
    public List<Partida> gerar(FaseTorneio fase, List<ParticipacaoFase> participantes) {
        if (fase.getFaseInicialMataMata() == null)
            throw new IllegalStateException("A fase de mata-mata precisa de uma fase inicial definida.");
        if (!Boolean.TRUE.equals(fase.getTemJogoVolta()))
            throw new IllegalStateException("O mata-mata de clãs exige jogo de ida e volta.");

        validarQuantidadeParticipantes(participantes.size(), fase);

        Map<String, List<ParticipacaoFase>> porEquipe = new LinkedHashMap<>();
        for (ParticipacaoFase p : participantes) {
            if (p.getEquipe() == null)
                throw new IllegalArgumentException("Todo participante precisa pertencer a uma equipe.");
            porEquipe.computeIfAbsent(p.getEquipe().getId(), k -> new ArrayList<>()).add(p); // getId() não inicializa o proxy
        }
        if (porEquipe.size() != 2)
            throw new IllegalArgumentException("O mata-mata de clãs exige exatamente 2 equipes.");

        Iterator<List<ParticipacaoFase>> it = porEquipe.values().iterator();
        List<ParticipacaoFase> a = it.next();
        List<ParticipacaoFase> b = it.next();
        if (a.size() != b.size() || a.size() % 2 != 0)
            throw new IllegalArgumentException("As duas equipes devem ter o mesmo número PAR de jogadores.");

        a.sort(POR_POSICAO);
        b.sort(POR_POSICAO);

        // sorteia qual equipe fica no lado esquerdo da chave
        if (random.nextBoolean()) {
            List<ParticipacaoFase> tmp = a;
            a = b;
            b = tmp;
        }

        int k = a.size();
        int porLado = k / 2;

        // Esquerdo: iEsq x (k+1-i)Dir | Direito: iDir x (k+1-i)Esq, com i = 1..k/2
        // Cada item é {melhor, pior}; o melhor é p1 (manda a volta), o pior é p2 (manda a ida)
        List<ParticipacaoFase[]> esquerdo = new ArrayList<>();
        List<ParticipacaoFase[]> direito = new ArrayList<>();
        List<String> logEsq = new ArrayList<>();
        List<String> logDir = new ArrayList<>();

        for (int i = 0; i < porLado; i++) {
            int pior = k - 1 - i;
            esquerdo.add(new ParticipacaoFase[]{a.get(i), b.get(pior)});
            logEsq.add("Clãs #" + (i + 1) + " x #" + (pior + 1));
            direito.add(new ParticipacaoFase[]{b.get(i), a.get(pior)});
            logDir.add("Clãs #" + (i + 1) + " x #" + (pior + 1));
        }

        // sorteia a posição dentro de cada lado (mantendo o par log/confronto junto)
        List<Integer> ordemEsq = embaralhado(porLado);
        List<Integer> ordemDir = embaralhado(porLado);

        List<Partida> partidas = new ArrayList<>();
        int chave = 1; // a base numera as chaves a partir de 1

        // chaves 1..k/2 formam a metade esquerda; k/2+1..k a metade direita
        for (int idx : ordemEsq) {
            ParticipacaoFase[] c = esquerdo.get(idx);
            partidas.addAll(criarConfronto(fase, chave++, c[0], c[1], logEsq.get(idx) + " (esq)"));
        }
        for (int idx : ordemDir) {
            ParticipacaoFase[] c = direito.get(idx);
            partidas.addAll(criarConfronto(fase, chave++, c[0], c[1], logDir.get(idx) + " (dir)"));
        }

        vincularProximasFases(partidas, fase);
        return partidas;
    }

    private List<Integer> embaralhado(int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(i);
        Collections.shuffle(l, random);
        return l;
    }
}