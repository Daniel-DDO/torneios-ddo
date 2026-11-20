package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.exception.*;
import com.ddo.torneios.model.*;
import com.ddo.torneios.repository.*;
import com.ddo.torneios.request.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepo;
    private final ParticipacaoFaseRepository participacaoRepo;
    private final JogadorClubeRepository jogadorClubeRepo;
    private final TorneioRepository torneioRepo;
    private final FaseTorneioRepository faseRepo;
    private final PartidaRepository partidaRepo;
    private final EntityManager em;

    @Transactional
    public EquipeResumoDTO criar(CriarEquipeRequest r) {
        String temporadaId = torneioRepo.buscarTemporadaId(r.torneioId())
                .orElseThrow(() -> new EntityNotFoundException("Torneio não encontrado."));
        String nome = r.nome().trim();

        if (equipeRepo.existsByTorneioIdAndNome(r.torneioId(), nome))
            throw new IllegalArgumentException("Já existe uma equipe com esse nome neste torneio.");
        if (!jogadorClubeRepo.existsByJogadorIdAndTemporadaId(r.capitaoId(), temporadaId))
            throw new IllegalArgumentException("O capitão precisa estar inscrito na temporada do torneio.");
        if (equipeRepo.existsByTorneioIdAndCapitaoId(r.torneioId(), r.capitaoId()))
            throw new IllegalArgumentException("Este jogador já é capitão de outra equipe neste torneio.");

        Equipe e = new Equipe();
        e.setNome(nome);
        e.setImagem(r.imagem());
        e.setTorneio(em.getReference(Torneio.class, r.torneioId()));
        e.setCapitao(em.getReference(Jogador.class, r.capitaoId()));
        equipeRepo.save(e);

        return equipeRepo.buscarResumo(e.getId()).orElseThrow();
    }

    @Transactional
    public EquipeResumoDTO editar(String equipeId, EditarEquipeRequest r) {
        EquipeRefDTO ref = equipeRepo.buscarRef(equipeId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));

        if (r.nome() != null && !r.nome().isBlank()) {
            String nome = r.nome().trim();
            if (equipeRepo.existsByTorneioIdAndNomeAndIdNot(ref.torneioId(), nome, equipeId))
                throw new IllegalArgumentException("Já existe uma equipe com esse nome neste torneio.");
            equipeRepo.atualizarNome(equipeId, nome);
        }
        if (r.imagem() != null) equipeRepo.atualizarImagem(equipeId, r.imagem());

        return equipeRepo.buscarResumo(equipeId).orElseThrow();
    }

    @Transactional
    public void trocarCapitao(String equipeId, String novoCapitaoId) {
        EquipeRefDTO ref = equipeRepo.buscarRef(equipeId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));

        if (novoCapitaoId.equals(ref.capitaoId())) return;
        if (!participacaoRepo.existsByEquipeIdAndJogadorClubeJogadorId(equipeId, novoCapitaoId))
            throw new IllegalArgumentException("O novo capitão precisa ser membro da equipe.");
        if (equipeRepo.existsByTorneioIdAndCapitaoId(ref.torneioId(), novoCapitaoId))
            throw new IllegalArgumentException("Este jogador já é capitão de outra equipe neste torneio.");

        equipeRepo.atualizarCapitao(equipeId, em.getReference(Jogador.class, novoCapitaoId));
    }

    @Transactional
    public void adicionarMembros(String equipeId, AdicionarMembrosRequest r) {
        EquipeRefDTO eq = equipeRepo.buscarRef(equipeId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
        FaseRefDTO fase = faseRepo.buscarRef(r.faseId())
                .orElseThrow(() -> new EntityNotFoundException("Fase não encontrada."));

        if (!fase.torneioId().equals(eq.torneioId()))
            throw new IllegalArgumentException("A equipe não pertence ao torneio desta fase.");
        if (partidaRepo.existsByFaseId(r.faseId()))
            throw new IllegalStateException("A fase já tem partidas geradas. O elenco não pode mais ser alterado.");

        List<String> ids = r.jogadorClubeIds().stream().distinct().toList();

        List<JogadorClubeTemporadaDTO> jcs = jogadorClubeRepo.buscarTemporadas(ids);
        if (jcs.size() != ids.size())
            throw new IllegalArgumentException("Algum jogador/clube informado não foi encontrado.");
        if (jcs.stream().anyMatch(j -> !j.temporadaId().equals(fase.temporadaId())))
            throw new IllegalArgumentException("Há jogador inscrito em outra temporada.");

        Map<String, ParticipacaoEquipeDTO> existentes = participacaoRepo
                .buscarPorFaseEJogadorClubes(r.faseId(), ids).stream()
                .collect(Collectors.toMap(ParticipacaoEquipeDTO::jogadorClubeId, Function.identity()));

        // buscarPorFaseEJogadorClubes usa p.equipe.id (join interno): participações sem equipe não aparecem.
        // Por isso o passo abaixo usa a lista completa de participações da fase para esses ids.
        // (ver observação 3 no fim)
        Equipe equipeRef = em.getReference(Equipe.class, equipeId);

        List<String> paraAtualizar = new ArrayList<>();
        for (ParticipacaoEquipeDTO p : existentes.values()) {
            if (p.equipeId() != null && !p.equipeId().equals(equipeId))
                throw new IllegalArgumentException("Um dos jogadores já pertence a outra equipe.");
            if (p.equipeId() == null) paraAtualizar.add(p.id());
        }
        if (!paraAtualizar.isEmpty()) participacaoRepo.definirEquipe(paraAtualizar, equipeRef);

        List<ParticipacaoFase> novas = new ArrayList<>();
        for (JogadorClubeTemporadaDTO j : jcs) {
            if (existentes.containsKey(j.id())) continue;
            ParticipacaoFase p = new ParticipacaoFase();
            p.setFase(em.getReference(FaseTorneio.class, r.faseId()));
            p.setJogadorClube(em.getReference(JogadorClube.class, j.id()));
            p.setEquipe(equipeRef);
            p.setStatusClassificacao(j.statusTemporada());
            novas.add(p);
        }
        if (!novas.isEmpty()) participacaoRepo.saveAll(novas);
    }

    @Transactional
    public void removerMembro(String equipeId, String participacaoId) {
        ParticipacaoEquipeDTO p = participacaoRepo.buscarParticipacaoComEquipe(participacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Participação não encontrada ou sem equipe."));
        if (!p.equipeId().equals(equipeId))
            throw new IllegalArgumentException("Esta participação não pertence à equipe informada.");

        EquipeRefDTO eq = equipeRepo.buscarRef(equipeId).orElseThrow();
        if (p.jogadorId().equals(eq.capitaoId()))
            throw new IllegalStateException("Troque o capitão antes de remover este jogador da equipe.");
        if (partidaRepo.existsByFaseId(p.faseId()))
            throw new IllegalStateException("A fase já tem partidas geradas. O elenco não pode mais ser alterado.");

        participacaoRepo.limparEquipe(participacaoId); // só desvincula; a participação na fase continua
    }

    @Transactional
    public void deletar(String equipeId) {
        if (!equipeRepo.existsById(equipeId)) throw new EntityNotFoundException("Equipe não encontrada.");
        if (participacaoRepo.existsByEquipeId(equipeId))
            throw new IllegalStateException("Remova todos os membros antes de excluir a equipe.");
        equipeRepo.deleteById(equipeId);
    }
}