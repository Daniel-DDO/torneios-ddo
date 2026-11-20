package com.ddo.torneios.service;

import com.ddo.torneios.dto.*;
import com.ddo.torneios.model.FaseTorneio;
import com.ddo.torneios.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipeConsultaService {

    private final EquipeRepository equipeRepo;
    private final ParticipacaoFaseRepository participacaoRepo;
    private final FaseTorneioRepository faseRepo;
    private final ClassificacaoEquipeService classificacaoEquipeService;

    @Transactional(readOnly = true)
    public List<EquipeTabelaoDTO> tabelao(String faseId) {
        FaseTorneio fase = fase(faseId);
        List<ClassificacaoEquipeDTO> tabela = classificacaoEquipeService.classificar(fase);
        Map<String, List<MembroEquipeDTO>> membros = participacaoRepo.membrosDaFase(faseId).stream()
                .collect(Collectors.groupingBy(MembroEquipeDTO::equipeId));

        return tabela.stream()
                .map(t -> new EquipeTabelaoDTO(t, membros.getOrDefault(t.equipeId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipeTabelaoDTO detalhe(String faseId, String equipeId) {
        FaseTorneio fase = fase(faseId);
        ClassificacaoEquipeDTO linha = classificacaoEquipeService.classificar(fase).stream()
                .filter(t -> t.equipeId().equals(equipeId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Equipe sem participação nesta fase."));
        return new EquipeTabelaoDTO(linha, participacaoRepo.membrosDaEquipe(faseId, equipeId));
    }

    /** Checagem antes de gerar a liga: devolve a lista de problemas (vazia = pronto). */
    @Transactional(readOnly = true)
    public List<String> validarElenco(String faseId) {
        List<EquipeContagemDTO> c = participacaoRepo.contagemPorEquipe(faseId);
        List<String> erros = new ArrayList<>();

        if (c.size() < 2) erros.add("São necessárias pelo menos 2 equipes com membros na fase.");
        if (c.stream().map(EquipeContagemDTO::membros).distinct().count() > 1)
            erros.add("As equipes têm quantidades diferentes de jogadores.");
        for (EquipeContagemDTO e : c) {
            if (e.membros() % 2 != 0) erros.add("A equipe " + e.nome() + " tem número ímpar de jogadores (" + e.membros() + ").");
            if (e.capitaesNoElenco() == 0) erros.add("O capitão da equipe " + e.nome() + " não está no elenco desta fase.");
        }
        return erros;
    }

    public List<EquipeResumoDTO> listarPorTorneio(String torneioId) {
        return equipeRepo.listarPorTorneio(torneioId);
    }

    public EquipeResumoDTO buscar(String equipeId) {
        return equipeRepo.buscarResumo(equipeId)
                .orElseThrow(() -> new EntityNotFoundException("Equipe não encontrada."));
    }

    public String imagem(String equipeId) {
        return equipeRepo.buscarImagem(equipeId).orElseThrow(() -> new EntityNotFoundException("Sem imagem."));
    }

    private FaseTorneio fase(String faseId) {
        if (!faseRepo.existsById(faseId)) throw new EntityNotFoundException("Fase não encontrada.");
        return faseRepo.getReferenceById(faseId); // proxy: só o id é usado nas queries
    }
}