package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.service.gerador.GeradorMataMataFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MataMataClasService {

    private final ClassificacaoEquipeService classificacaoEquipeService;
    private final ParticipacaoFaseRepository participacaoRepo;
    private final JogadorClubeRepository jogadorClubeRepo;
    private final PartidaRepository partidaRepo;
    private final GeradorMataMataFactory factory;
    private final EntityManager em;

    @Transactional
    public void gerar(FaseTorneio faseMataMata, FaseTorneio faseLiga) {
        if (participacaoRepo.existsByFaseId(faseMataMata.getId()))
            throw new IllegalStateException("O mata-mata já foi gerado para esta fase.");

        List<ParticipacaoFase> participantes = selecionarParticipantes(faseMataMata, faseLiga);
        List<Partida> partidas = factory.obterEstrategia(AlgoritmoGeracaoMataMata.TORNEIO_CLAS)
                .gerar(faseMataMata, participantes);
        partidaRepo.saveAll(partidas); // proximaPartida tem cascade ALL
    }

    /** Pega as 2 melhores equipes e o top-N individual de cada uma (N = menor elenco), já salvos na fase do mata-mata. */
    @Transactional
    public List<ParticipacaoFase> selecionarParticipantes(FaseTorneio faseMataMata, FaseTorneio faseLiga) {
        List<ClassificacaoEquipeDTO> tabela = classificacaoEquipeService.classificar(faseLiga);
        if (tabela.size() < 2) throw new IllegalStateException("São necessárias pelo menos 2 equipes.");

        String idA = tabela.get(0).equipeId();
        String idB = tabela.get(1).equipeId();

        List<ParticipanteRankDTO> a = new ArrayList<>(), b = new ArrayList<>();
        for (ParticipanteRankDTO p : participacaoRepo.rankingIndividualDasEquipes(faseLiga.getId(), List.of(idA, idB))) {
            if (p.posicao() == null)
                throw new IllegalStateException("A classificação individual da liga ainda não foi fechada.");
            (p.equipeId().equals(idA) ? a : b).add(p); // já vem ordenado por posição
        }

        int n = Math.min(a.size(), b.size());
        List<ParticipanteRankDTO> escolhidos = new ArrayList<>(a.subList(0, n));
        escolhidos.addAll(b.subList(0, n));

        // uma query só para carregar os JogadorClube (a base lê clube/estádio do mandante)
        Map<String, JogadorClube> jcs = jogadorClubeRepo.findAllById(
                        escolhidos.stream().map(ParticipanteRankDTO::jogadorClubeId).toList())
                .stream().collect(Collectors.toMap(JogadorClube::getId, Function.identity()));

        List<ParticipacaoFase> novas = new ArrayList<>();
        for (ParticipanteRankDTO p : escolhidos) {
            ParticipacaoFase pf = new ParticipacaoFase();
            pf.setFase(faseMataMata);
            pf.setJogadorClube(jcs.get(p.jogadorClubeId()));
            pf.setEquipe(em.getReference(Equipe.class, p.equipeId()));
            pf.setPosicaoClassificacao(p.posicao());
            pf.setStatusClassificacao(StatusClassificacao.ATIVO);
            novas.add(pf);
        }
        return participacaoRepo.saveAll(novas);
    }
}